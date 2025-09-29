package com.example.echoshotx.infrastructure.ai.dto.response;

import com.example.echoshotx.domain.video.entity.ProcessingStatus;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessingResponse {
    
    private Long videoId;
    private ProcessingType processingType;
    private ProcessingStatus status;
    private String outputVideoUrl;
    private String thumbnailUrl;
    private ProcessingResult result;
    private LocalDateTime completedAt;
    private String errorMessage;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingResult {
        private Integer processingTime; // 처리 시간 (초)
        private String outputFormat;
        private Long outputFileSize;
        private Integer outputWidth;
        private Integer outputHeight;
        private Double outputFrameRate;
        private List<String> processingStages; // 처리 단계별 로그
    }
}
