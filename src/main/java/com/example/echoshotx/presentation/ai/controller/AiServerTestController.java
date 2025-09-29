package com.example.echoshotx.presentation.ai.controller;

import com.example.echoshotx.domain.video.entity.ProcessingStatus;
import com.example.echoshotx.infrastructure.service.AiServerClient;
import com.example.echoshotx.infrastructure.ai.dto.request.VideoProcessingRequest;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
public class AiServerTestController {
    
    private final AiServerClient aiServerClient;
    
    /**
     * AI 서버 헬스체크 테스트
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return aiServerClient.healthCheck()
            .map(healthy -> healthy ? "AI 서버 정상" : "AI 서버 비정상")
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(500).body("AI 서버 연결 실패"))
            .block();
    }
    
    /**
     * 영상처리 요청 테스트
     */
    @PostMapping("/video/process")
    public ResponseEntity<VideoProcessingResponse> testVideoProcessing(
            @RequestParam Long videoId,
            @RequestParam String processingType) {
        
        try {
            VideoProcessingRequest request = VideoProcessingRequest.builder()
                .videoId(videoId)
                .inputVideoUrl("https://example.com/test-video.mp4")
                .outputPath("result/" + videoId + "/C_video.mp4")
                .processingType(com.example.echoshotx.domain.video.entity.ProcessingType.valueOf(processingType))
                .metadata(createTestMetadata())
                .requestedAt(LocalDateTime.now())
                .build();
            
            return aiServerClient.processVideo(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500).body(createErrorResponse()))
                .block();
                
        } catch (Exception e) {
            log.error("영상처리 테스트 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse());
        }
    }
    
    /**
     * 영상처리 상태 조회 테스트
     */
    @GetMapping("/video/status/{videoId}")
    public ResponseEntity<VideoProcessingResponse> getVideoProcessingStatus(@PathVariable Long videoId) {
        return aiServerClient.getVideoProcessingStatus(videoId)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(500).body(createErrorResponse()))
            .block();
    }
    
    /**
     * 테스트용 메타데이터 생성
     */
    private VideoProcessingRequest.VideoMetadata createTestMetadata() {
        return VideoProcessingRequest.VideoMetadata.builder()
            .fileName("test-video.mp4")
            .fileSize(1024L * 1024 * 100) // 100MB
            .mimeType("video/mp4")
            .duration(300) // 5분
            .width(1920)
            .height(1080)
            .frameRate(30.0)
            .build();
    }
    
    /**
     * 에러 응답 생성
     */
    private VideoProcessingResponse createErrorResponse() {
        return VideoProcessingResponse.builder()
            .videoId(0L)
            .status(ProcessingStatus.FAILED)
            .errorMessage("AI 서버 연결 실패")
            .build();
    }
}
