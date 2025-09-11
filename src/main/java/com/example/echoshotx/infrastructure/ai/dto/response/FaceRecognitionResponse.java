package com.example.echoshotx.infrastructure.ai.dto.response;

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
public class FaceRecognitionResponse {
    
    private Long videoId;
    private ProcessingStatus status;
    private String outputDataUrl;
    private FaceRecognitionResult result;
    private LocalDateTime completedAt;
    private String errorMessage;
    
    public enum ProcessingStatus {
        PROCESSING, SUCCEEDED, FAILED, CANCELLED
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceRecognitionResult {
        private Integer totalFrames;
        private Integer processedFrames;
        private List<FaceData> faces;
        private Integer processingTime; // 처리 시간 (초)
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceData {
        private Integer frameNumber;
        private Double timestamp; // 초 단위
        private Double x; // 좌상단 X 좌표
        private Double y; // 좌상단 Y 좌표
        private Double width; // 얼굴 너비
        private Double height; // 얼굴 높이
        private Double confidence; // 신뢰도
        private String faceId; // 얼굴 ID (추적용)
    }
}
