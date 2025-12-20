package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.notification.application.event.VideoProcessingCompletedEvent;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.vo.ProcessedVideo;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.request.WebhookProcessingCompletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 서버에서 처리 완료 시 호출하는 Webhook UseCase.
 *
 * <ul>
 *   <li>처리된 영상 정보 업데이트</li>
 *   <li>상태 변경 (QUEUED/PROCESSING → COMPLETED)</li>
 *   <li>처리 완료 알림 발송</li>
 * </ul>
 */
@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ProcessingCompletedWebhookUseCase {

    private final VideoAdaptor videoAdaptor;
    private final VideoService videoService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(WebhookProcessingCompletedRequest request) {
        // 1. 비디오 조회
        Video video = videoAdaptor.queryById(request.getVideoId());
        log.info(
                "Processing completed webhook received: videoId={}, aiJobId={}",
                request.getVideoId(),
                request.getAiJobId());

        // 2. ProcessedVideo 생성
        ProcessedVideo processedVideo = createProcessedVideo(request);

        // 3. Processed VideoMetadata 생성
        VideoMetadata processedMetadata = createProcessedMetadata(request);

        // 4. 처리 완료 (QUEUED/PROCESSING → COMPLETED)
        videoService.completeProcessing(video, processedVideo, processedMetadata);
        log.info("Video processing completed successfully: videoId={}", request.getVideoId());

        // 처리 완료 이벤트 발행
        VideoProcessingCompletedEvent event = new VideoProcessingCompletedEvent(
                video.getId(),
                video.getMemberId(),
                video.getOriginalFile().getFileName());
        publishCompleteEvent(event);

        // 5. 썸네일 저장 (옵셔널)
        if (request.getThumbnailS3Key() != null) {
            // TODO: 썸네일 저장 로직
            log.info(
                    "Thumbnail saved: videoId={}, thumbnailKey={}",
                    request.getVideoId(),
                    request.getThumbnailS3Key());
        }
    }

    private ProcessedVideo createProcessedVideo(WebhookProcessingCompletedRequest request) {
        return ProcessedVideo.builder()
                .s3Key(request.getProcessedS3Key())
                .fileSizeBytes(request.getProcessedFileSizeBytes())
                .build();
    }

    private VideoMetadata createProcessedMetadata(WebhookProcessingCompletedRequest request) {
        return VideoMetadata.builder()
                .durationSeconds(request.getProcessedDurationSeconds())
                .width(request.getProcessedWidth())
                .height(request.getProcessedHeight())
                .codec(request.getProcessedCodec())
                .bitrate(request.getProcessedBitrate())
                .frameRate(request.getProcessedFrameRate())
                .build();
    }

    private void publishCompleteEvent(VideoProcessingCompletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
