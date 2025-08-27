package com.example.echoshotx.domain.video.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VideoStatus {
    UPLOADED("업로드 완료"),
    PROCESSING("처리 중"),
    PROCESSED("처리 완료"),
    FAILED("처리 실패"),
    ARCHIVED("보관됨");

    private final String description;

}
