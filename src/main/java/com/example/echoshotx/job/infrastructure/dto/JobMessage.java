package com.example.echoshotx.job.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class JobMessage {

    /**
     * Job pk
     */
    private final Long jobId;

    /**
     * 업스케일 대상이 되는 S3 오브젝트 키
     * 예) uploads/videos/2025/11/10/abc.mp4
     */
    private final String s3Key;

    /**
     * 작업 유형 (예: UPSCALE, THUMBNAIL_GENERATION 등)
     */
    private final String taskType;

    /**
     * 작업을 요청한 사용자 ID
     */
    private final Long memberId;

    /**
     * 비디오 ID
     */
    private final Long videoId;
}
