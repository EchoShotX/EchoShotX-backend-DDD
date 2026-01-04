package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.exception.VideoErrorStatus;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.CompleteUploadResponse;

import java.util.UUID;

import com.example.echoshotx.video.presentation.exception.VideoHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트용: 비관락을 사용하지 않는 CompleteVideoUploadUseCase 변형.
 * 
 * <p>비관락 적용 전후 비교 테스트를 위해 사용됩니다.
 * 실제 코드는 변경하지 않고, 테스트에서만 사용하는 클래스입니다.
 */
@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class CompleteVideoUploadUseCaseWithoutLock {

    private final VideoAdaptor videoAdaptor;
    private final VideoService videoService;
    private final CreditService creditService;
    private final JobService jobService;
    private final JobEventHandler jobEventHandler;

    public CompleteUploadResponse execute(
            Long videoId, CompleteUploadRequest request, Member member) {

        // 1. 비디오 조회 및 권한 검증 (비관락 없이 조회)
        Video video = videoAdaptor.queryById(videoId);
        video.validateMember(member);

        // 2. 상태 검증
        if (video.getStatus() != VideoStatus.PENDING_UPLOAD) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_ALREADY_PROCESSED);
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

        return CompleteUploadResponse.from(video);
    }

    private String sendToProcessingQueue(Member member, Video video) {
		Job job = jobService.createJob(member, video.getId(), video.getOriginalFile().getS3Key(), video.getProcessingType());
		JobCreatedEvent event = JobCreatedEvent.builder()
				.jobId(job.getId())
				.videoId(job.getVideoId())
				.processingType(job.getProcessingType())
				.memberId(member.getId())
				.s3Key(job.getS3Key())
				.build();
		jobEventHandler.handleCreate(event);
        // Mock implementation
        return UUID.randomUUID().toString();
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

