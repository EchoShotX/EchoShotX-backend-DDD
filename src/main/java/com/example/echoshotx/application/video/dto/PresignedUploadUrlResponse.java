package com.example.echoshotx.application.video.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PresignedUploadUrlResponse {

    private String uploadUrl;
    private String s3Key;
    private LocalDateTime expiresAt;
    private String contentType;
    private Long maxSizeBytes;

}
