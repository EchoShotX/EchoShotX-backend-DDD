package com.example.echoshotx.job.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
    REQUESTED("Job 요청 접수됨"),
    PUBLISHED("Job 게시됨"),
    FAILED("Job 실패함"),;

    private final String description;


}
