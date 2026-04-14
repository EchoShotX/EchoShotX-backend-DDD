package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;

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
    private static final String JOB_MESSAGE_ID_PREFIX = "job:";

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
        String requestHash = createRequestHashIfNeeded(normalizedKey, videoId, request);

        Optional<CompleteUploadResponse> cachedResponse =
                findCachedResponse(normalizedKey, member.getId(), videoId, requestHash);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        if (normalizedKey == null) {
            return processCompleteUpload(videoId, request, member, null, null);
        }

        String lockKey = REDIS_LOCK_KEY_PREFIX + videoId;
        String lockToken = UUID.randomUUID().toString();
        boolean acquired = tryAcquireRedisLock(lockKey, lockToken);

        if (!acquired) {
            Optional<CompleteUploadResponse> retryCachedResponse =
                    findCachedResponse(normalizedKey, member.getId(), videoId, requestHash);
            if (retryCachedResponse.isPresent()) {
                return retryCachedResponse.get();
            }
            log.info("Redis lock not acquired. continue with DB lock. key={}", lockKey);
        }

        try {
            return processCompleteUpload(videoId, request, member, normalizedKey, requestHash);
        } finally {
            releaseRedisLockIfNeeded(acquired, lockKey, lockToken);
        }
    }

    private CompleteUploadResponse processCompleteUpload(
            Long videoId,
            CompleteUploadRequest request,
            Member member,
            String normalizedKey,
            String requestHash) {

        Video video = videoAdaptor.queryByIdWithLock(videoId);
        video.validateMember(member);

        if (video.getStatus() != VideoStatus.PENDING_UPLOAD) {
            return handleAlreadyProcessedVideo(video, member.getId(), videoId, normalizedKey, requestHash);
        }

        VideoMetadata metadata = createVideoMetadata(request);
        video = videoService.completeUpload(video, metadata);
        creditService.useCreditsForVideoProcessing(member, video, video.getProcessingType());
        String sqsMessageId = sendToProcessingQueue(member, video);
        log.info("Video sent to processing queue: videoId={}, sqsMessageId={}", videoId, sqsMessageId);
        video = videoService.enqueueForProcessing(video, sqsMessageId);

        CompleteUploadResponse response = CompleteUploadResponse.from(video);
        cacheSuccessResponseIfNeeded(member.getId(), videoId, normalizedKey, requestHash, response);

        return response;
    }

    private CompleteUploadResponse handleAlreadyProcessedVideo(
            Video video,
            Long memberId,
            Long videoId,
            String normalizedKey,
            String requestHash) {
        if (normalizedKey == null) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_ALREADY_PROCESSED);
        }

        CompleteUploadResponse response = CompleteUploadResponse.from(video);
        cacheSuccessResponseIfNeeded(memberId, videoId, normalizedKey, requestHash, response);
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
        return JOB_MESSAGE_ID_PREFIX + job.getId();
    }

    private String createRequestHashIfNeeded(
            String normalizedKey, Long videoId, CompleteUploadRequest request) {
        if (normalizedKey == null) {
            return null;
        }

        return idempotencyService.createRequestHash(videoId, request);
    }

    private Optional<CompleteUploadResponse> findCachedResponse(
            String normalizedKey,
            Long memberId,
            Long videoId,
            String requestHash) {
        if (normalizedKey == null) {
            return Optional.empty();
        }

        return idempotencyService.findSuccessResponse(memberId, videoId, normalizedKey, requestHash);
    }

    private boolean tryAcquireRedisLock(String lockKey, String lockToken) {
        try {
            return redisLockService.tryLock(lockKey, lockToken, REDIS_LOCK_TTL);
        } catch (RuntimeException e) {
            log.warn("Redis lock acquire failed. fallback to DB lock. key={}", lockKey, e);
            return false;
        }
    }

    private void releaseRedisLockIfNeeded(boolean acquired, String lockKey, String lockToken) {
        if (!acquired) {
            return;
        }

        try {
            redisLockService.unlock(lockKey, lockToken);
        } catch (RuntimeException e) {
            log.warn("Redis unlock failed. key={}", lockKey, e);
        }
    }

    private void cacheSuccessResponseIfNeeded(
            Long memberId,
            Long videoId,
            String normalizedKey,
            String requestHash,
            CompleteUploadResponse response) {
        if (normalizedKey == null) {
            return;
        }

        idempotencyService.saveSuccessResponse(memberId, videoId, normalizedKey, requestHash, response);
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
