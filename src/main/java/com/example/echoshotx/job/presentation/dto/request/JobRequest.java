package com.example.echoshotx.job.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class JobRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        private String s3Key;
        private String taskType;
    }

}
