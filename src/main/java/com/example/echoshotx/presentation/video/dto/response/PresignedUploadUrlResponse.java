package com.example.echoshotx.presentation.video.dto.response;

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
