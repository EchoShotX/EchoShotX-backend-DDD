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
public class MusicAnalysisRequest {
    
    private Long videoId;
    private String inputAudioUrl;
    private String outputPath;
    private MusicAnalysisConfig config;
    private LocalDateTime requestedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicAnalysisConfig {
        private Integer clipLengths; // 클립 길이 (15, 30, 60초)
        private Double energyThreshold; // 에너지 임계값
        private Boolean detectChorus; // 후렴구 감지 여부
        private Boolean detectBridge; // 브릿지 감지 여부
        private Boolean detectIntro; // 도입부 감지 여부
    }
}
