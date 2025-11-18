package com.example.echoshotx.job.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class JobRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        // video
        private Long videoId;
        // 영상 관련 메타데이터 추가 가능
        private String s3Key;
        private String taskType;
    }

}
