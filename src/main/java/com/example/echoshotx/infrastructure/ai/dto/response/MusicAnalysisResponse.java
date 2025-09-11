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
public class MusicAnalysisResponse {
    
    private Long videoId;
    private ProcessingStatus status;
    private String outputDataUrl;
    private MusicAnalysisResult result;
    private LocalDateTime completedAt;
    private String errorMessage;
    
    public enum ProcessingStatus {
        PROCESSING, SUCCEEDED, FAILED, CANCELLED
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicAnalysisResult {
        private Integer totalDuration; // 총 길이 (초)
        private List<HighlightClip> highlightClips;
        private MusicStructure structure;
        private Integer processingTime; // 처리 시간 (초)
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightClip {
        private String clipId;
        private Integer startTime; // 시작 시간 (초)
        private Integer endTime; // 종료 시간 (초)
        private Integer duration; // 길이 (초)
        private ClipType type;
        private Double energy; // 에너지 레벨
        private Double confidence; // 신뢰도
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicStructure {
        private List<Section> sections;
        private Integer introStart;
        private Integer introEnd;
        private Integer chorusStart;
        private Integer chorusEnd;
        private Integer bridgeStart;
        private Integer bridgeEnd;
        private Integer outroStart;
        private Integer outroEnd;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String name;
        private Integer startTime;
        private Integer endTime;
        private Double energy;
        private SectionType type;
    }
    
    public enum ClipType {
        INTRO, CHORUS, BRIDGE, OUTRO, HIGH_ENERGY, CUSTOM
    }
    
    public enum SectionType {
        INTRO, VERSE, CHORUS, BRIDGE, OUTRO, INSTRUMENTAL
    }
}
