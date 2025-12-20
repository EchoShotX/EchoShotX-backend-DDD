package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.credit.domain.util.CreditCalculator;
import com.example.echoshotx.notification.application.event.VideoProcessingFailedEvent;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.presentation.dto.request.WebhookProcessingFailedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 서버에서 처리 실패 시 호출하는 Webhook UseCase.
 *
 * <ul>
 *   <li>상태 변경 (QUEUED/PROCESSING → FAILED)</li>
 *   <li>처리 실패 알림 발송</li>
 *   <li>크레딧 환불</li>
 * </ul>
 */
@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ProcessingFailedWebhookUseCase {

    private final VideoAdaptor videoAdaptor;
    private final VideoService videoService;
    private final CreditService creditService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(WebhookProcessingFailedRequest request) {
        // 1. 비디오 조회
        Video video = videoAdaptor.queryById(request.getVideoId());
        log.warn(
                "Processing failed webhook received: videoId={}, aiJobId={}, error={}",
                request.getVideoId(),
                request.getAiJobId(),
                request.getErrorMessage());

        // 2. 사용된 크레딧 계산 (환불을 위해)
        int usedCredits = CreditCalculator.calculateRequiredCredits(video.getProcessingType(), video.getOriginalMetadata().getDurationSeconds());

        // 3. 처리 실패 및 알림 발행 (QUEUED/PROCESSING → FAILED)
        String errorMessage = createErrorMessage(request);
        videoService.failProcessing(video, errorMessage);

        // 처리 실패 이벤트 발행
        eventPublisher.publishEvent(
                new VideoProcessingFailedEvent(
                        video.getId(),
                        video.getMemberId(),
                        video.getOriginalFile().getFileName(),
                        errorMessage));
        log.info(
                "Video processing failed: videoId={}, retryCount={}",
                request.getVideoId(),
                video.getRetryCount());

        // 4. 크레딧 환불
        if (usedCredits > 0) {
            creditService.refundCredits(
                    video.getMemberId(), video.getId(), usedCredits, "영상 처리 실패로 인한 크레딧 환불");
            log.info("Credits refunded: videoId={}, amount={}", request.getVideoId(), usedCredits);
        }
    }


    private String createErrorMessage(WebhookProcessingFailedRequest request) {
        return String.format(
                "[%s] %s",
                request.getErrorCode() != null ? request.getErrorCode() : "UNKNOWN",
                request.getErrorMessage());
    }

}
