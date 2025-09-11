package com.example.echoshotx.presentation.ai.dto.webhook;

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
public class VideoProcessingCompletedWebhook {
    
    @NotNull
    @Positive
    private Long videoId;
    
    @NotNull
    @Positive
    private Long memberId;
    
    @NotNull
    private VideoProcessingResponse.ProcessingStatus status;
    
    private String outputVideoUrl;
    private String thumbnailUrl;
    private String errorMessage;
    
    @NotNull
    @Positive
    private Integer creditsUsed;
}
