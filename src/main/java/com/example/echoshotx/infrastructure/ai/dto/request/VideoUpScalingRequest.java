package com.example.echoshotx.infrastructure.ai.dto.request;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUpScalingRequest {
    
    private Long videoId;
    private String inputVideoUrl;
    private String outputPath;
    private ProcessingType processingType;
    private VideoMetadata metadata;
    private LocalDateTime requestedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoMetadata {
        private String fileName;
        private Long fileSize;
        private String mimeType;
        private Integer duration; // 초 단위
        private Integer width;
        private Integer height;
        private Double frameRate;
    }
}
