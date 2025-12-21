package com.example.echoshotx.video.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcessingType {
    AI_UPSCALING("AI 업스케일링", 10); // add strategy pattern later

    private final String description;
    private final int creditCostPerSecond;

}
