package com.example.echoshotx.job.application.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobCreatedEvent {
    private final Long jobId;
    private final Long memberId;
    private final Long videoId;
    private final String s3Key;
    private final String taskType;
}