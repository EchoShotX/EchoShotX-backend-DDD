package com.example.echoshotx.presentation.ai.dto.webhook;

import com.example.echoshotx.infrastructure.ai.dto.response.MusicAnalysisResponse;
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
public class MusicAnalysisCompletedWebhook {
    
    @NotNull
    @Positive
    private Long videoId;
    
    @NotNull
    @Positive
    private Long memberId;
    
    @NotNull
    private MusicAnalysisResponse.ProcessingStatus status;
    
    private String outputDataUrl;
    private String errorMessage;
    
    @NotNull
    @Positive
    private Integer creditsUsed;
}
