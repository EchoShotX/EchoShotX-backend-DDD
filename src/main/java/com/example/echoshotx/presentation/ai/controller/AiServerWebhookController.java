package com.example.echoshotx.presentation.ai.controller;

import com.example.echoshotx.domain.credit.service.CreditService;
import com.example.echoshotx.domain.video.service.VideoService;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
import com.example.echoshotx.infrastructure.ai.dto.response.FaceRecognitionResponse;
import com.example.echoshotx.infrastructure.ai.dto.response.MusicAnalysisResponse;
import com.example.echoshotx.presentation.ai.dto.webhook.VideoProcessingCompletedWebhook;
import com.example.echoshotx.presentation.ai.dto.webhook.FaceRecognitionCompletedWebhook;
import com.example.echoshotx.presentation.ai.dto.webhook.MusicAnalysisCompletedWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/webhook/ai")
@RequiredArgsConstructor
public class AiServerWebhookController {
    
    private final VideoService videoService;
    private final CreditService creditService;
    
    /**
     * 영상처리 완료 웹훅
     */
    @PostMapping("/video/processing-completed")
    public ResponseEntity<Void> handleVideoProcessingCompleted(
            @Valid @RequestBody VideoProcessingCompletedWebhook webhook) {
        
        try {
            log.info("영상처리 완료 웹훅 수신: videoId={}, status={}", 
                    webhook.getVideoId(), webhook.getStatus());
            
            // 1. Video 엔티티 업데이트
            videoService.updateVideoProcessingResult(
                webhook.getVideoId(), 
                webhook.getStatus(), 
                webhook.getOutputVideoUrl(),
                webhook.getThumbnailUrl()
            );
            
            // 2. 크레딧 차감 (성공 시)
            if (webhook.getStatus() == VideoProcessingResponse.ProcessingStatus.SUCCEEDED) {
                creditService.deductCredits(
                    webhook.getMemberId(), 
                    webhook.getCreditsUsed(),
                    "영상처리 완료"
                );
            } else if (webhook.getStatus() == VideoProcessingResponse.ProcessingStatus.FAILED) {
                // 3. 실패 시 크레딧 환불
                creditService.refundCredits(
                    webhook.getMemberId(), 
                    webhook.getCreditsUsed(),
                    "영상처리 실패 환불"
                );
            }
            
            log.info("영상처리 완료 웹훅 처리 성공: videoId={}", webhook.getVideoId());
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("영상처리 완료 웹훅 처리 실패: videoId={}, error={}", 
                    webhook.getVideoId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * AI 서버 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI 서버 웹훅 엔드포인트 정상");
    }
}
