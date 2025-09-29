package com.example.echoshotx.presentation.ai.dto.webhook;

import com.example.echoshotx.domain.video.entity.ProcessingStatus;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUpScalingCompletedWebhook {
    
    @NotNull
    @Positive
    private Long videoId;
    
    @NotNull
    @Positive
    private Long memberId;
    
    @NotNull
    private ProcessingStatus status;
    
    private String outputVideoUrl;
    private String thumbnailUrl;
    private String errorMessage;
    
    @NotNull
    @Positive
    private Integer creditsUsed;
}
