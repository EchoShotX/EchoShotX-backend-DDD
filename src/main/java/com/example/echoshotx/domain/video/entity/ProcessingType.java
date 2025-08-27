package com.example.echoshotx.domain.video.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcessingType {
    BASIC_ENHANCEMENT("기본 향상", 1),
    AI_UPSCALING("AI 업스케일링", 3); // add strategy pattern later

    private final String description;
    private final int tokenCostPerSecond;


}
