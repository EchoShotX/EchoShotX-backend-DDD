package com.example.echoshotx.job.application.event;

import com.example.echoshotx.video.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class JobCreatedEvent {
    private final Long jobId;
    private final Long memberId;
    private final Long videoId;
    private final String s3Key;
    private final ProcessingType processingType;
}