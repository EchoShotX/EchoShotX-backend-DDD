package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.job.application.service.JobOutboxService;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.redis.service.RedisLockService;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoUploadIdempotencyService;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.exception.VideoErrorStatus;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.CompleteUploadResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.example.echoshotx.video.presentation.exception.VideoHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 클라이언트가 S3 업로드 완료 후 호출하는 UseCase.
 *
 * <ul>
 *   <li>업로드 완료 처리</li>
 *   <li>크레딧 차감</li>
 *   <li>SQS 큐에 메시지 전송</li>
 *   <li>처리 시작 알림 발송</li>
 * </ul>
 */
@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class CompleteVideoUploadUseCase {

    private static final Duration REDIS_LOCK_TTL = Duration.ofSeconds(20);
    private static final String REDIS_LOCK_KEY_PREFIX = "video:complete:";

    private final VideoAdaptor videoAdaptor;
    private final VideoService videoService;
    private final CreditService creditService;
    private final JobService jobService;
    private final JobOutboxService jobOutboxService;
    private final VideoUploadIdempotencyService idempotencyService;
    private final RedisLockService redisLockService;

    public CompleteUploadResponse execute(
            Long videoId, CompleteUploadRequest request, Member member) {
        return execute(videoId, request, member, null);
    }

    public CompleteUploadResponse execute(
            Long videoId,
            CompleteUploadRequest request,
            Member member,
            String idempotencyKey) {

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = null;
        if (normalizedKey != null) {
            requestHash = idempotencyService.createRequestHash(videoId, request);
            Optional<CompleteUploadResponse> cachedResponse =
                    idempotencyService.findSuccessResponse(
                            member.getId(), videoId, normalizedKey, requestHash);
            if (cachedResponse.isPresent()) {
                return cachedResponse.get();
            }
        }

        if (normalizedKey == null) {
            return processCompleteUpload(videoId, request, member, null, null);
        }

        String lockKey = REDIS_LOCK_KEY_PREFIX + videoId;
        String lockToken = UUID.randomUUID().toString();
        boolean acquired = false;

        try {
            acquired = redisLockService.tryLock(lockKey, lockToken, REDIS_LOCK_TTL);
        } catch (RuntimeException e) {
            log.warn("Redis lock acquire failed. fallback to DB lock. key={}", lockKey, e);
        }

        if (!acquired) {
            Optional<CompleteUploadResponse> retryCachedResponse =
                    idempotencyService.findSuccessResponse(
                            member.getId(), videoId, normalizedKey, requestHash);
            if (retryCachedResponse.isPresent()) {
                return retryCachedResponse.get();
            }
            log.info("Redis lock not acquired. continue with DB lock. key={}", lockKey);
        }

        try {
            return processCompleteUpload(videoId, request, member, normalizedKey, requestHash);
        } finally {
            if (acquired) {
                try {
                    redisLockService.unlock(lockKey, lockToken);
                } catch (RuntimeException e) {
                    log.warn("Redis unlock failed. key={}", lockKey, e);
                }
            }
        }
    }

    private CompleteUploadResponse processCompleteUpload(
            Long videoId,
            CompleteUploadRequest request,
            Member member,
            String normalizedKey,
            String requestHash) {

        // 1. 비디오 조회 및 권한 검증
        Video video = videoAdaptor.queryByIdWithLock(videoId);
        video.validateMember(member);

        // 2. 상태 검증
        if (video.getStatus() != VideoStatus.PENDING_UPLOAD) {
            if (normalizedKey == null) {
                throw new VideoHandler(VideoErrorStatus.VIDEO_ALREADY_PROCESSED);
            }

            CompleteUploadResponse response = CompleteUploadResponse.from(video);
            idempotencyService.saveSuccessResponse(
                    member.getId(), videoId, normalizedKey, requestHash, response);
            return response;
        }

        VideoMetadata metadata = createVideoMetadata(request);
        // 3. 업로드 완료 처리
        video = videoService.completeUpload(video, metadata);

        // 4. 크레딧 차감
        creditService.useCreditsForVideoProcessing(member, video, video.getProcessingType());

        // 5. SQS에 메시지 전송
        String sqsMessageId = sendToProcessingQueue(member, video);
        log.info("Video sent to processing queue: videoId={}, sqsMessageId={}", videoId, sqsMessageId);

        // 6. 처리 대기열에 추가 및 알림 발행 (UPLOAD_COMPLETED → QUEUED)
        video = videoService.enqueueForProcessing(video, sqsMessageId);

        CompleteUploadResponse response = CompleteUploadResponse.from(video);

        if (normalizedKey != null) {
            idempotencyService.saveSuccessResponse(
                    member.getId(), videoId, normalizedKey, requestHash, response);
        }

        return response;
    }

    private String sendToProcessingQueue(Member member, Video video) {
        Job job =
                jobService.createJob(
                        member,
                        video.getId(),
                        video.getOriginalFile().getS3Key(),
                        video.getProcessingType());
        jobOutboxService.enqueueJobCreated(job, member.getId());
        return "job:" + job.getId();
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return idempotencyKey.trim();
    }

    private VideoMetadata createVideoMetadata(CompleteUploadRequest request) {
        return VideoMetadata.builder()
                .durationSeconds(request.getDurationSeconds())
                .width(request.getWidth())
                .height(request.getHeight())
                .codec(request.getCodec())
                .bitrate(request.getBitrate())
                .frameRate(request.getFrameRate())
                .build();
    }
}
