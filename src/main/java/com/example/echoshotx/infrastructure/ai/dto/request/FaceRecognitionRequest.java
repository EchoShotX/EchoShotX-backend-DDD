package com.example.echoshotx.infrastructure.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceRecognitionRequest {
    
    private Long videoId;
    private String inputVideoUrl;
    private String outputPath;
    private FaceRecognitionConfig config;
    private LocalDateTime requestedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceRecognitionConfig {
        private Double confidenceThreshold; // 0.0 ~ 1.0
        private Integer maxFaces; // 최대 인식할 얼굴 수
        private Boolean trackFaces; // 얼굴 추적 여부
        private Integer frameInterval; // 처리할 프레임 간격 (1 = 모든 프레임)
    }
}
