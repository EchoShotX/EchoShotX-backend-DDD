package com.example.echoshotx.domain.video.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcessingStatus {
    // PROCESSING, SUCCEEDED, FAILED, CANCELLED
    PROCESSING("처리 중"),
    SUCCEEDED("처리 완료"),
    FAILED("처리 실패"),
    CANCELLED("처리 취소");

    private final String description;
    
}
