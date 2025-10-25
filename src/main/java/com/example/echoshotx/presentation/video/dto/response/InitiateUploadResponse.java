package com.example.echoshotx.presentation.video.dto.response;

import com.example.echoshotx.application.video.dto.PresignedUploadUrlResponse;
import com.example.echoshotx.domain.video.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateUploadResponse {

    private Long videoId;              // 생성된 Video ID
    private String uploadId;           // Upload 추적 ID
    private String uploadUrl;          // Presigned URL
    private String s3Key;              // S3 키
    private LocalDateTime expiresAt;   // URL 만료 시간
    private String contentType;        // Content-Type (클라이언트가 PUT 요청 시 사용)
    private Long maxSizeBytes;         // 최대 크기

    public static InitiateUploadResponse from(
            Video video, PresignedUploadUrlResponse urlResponse,
            String uploadId, String s3Key, String contentType, Long filesSizeBytes
    ) {
        return InitiateUploadResponse.builder()
                .videoId(video.getId())
                .uploadId(uploadId)
                .uploadUrl(urlResponse.getUploadUrl())
                .s3Key(s3Key)
                .expiresAt(urlResponse.getExpiresAt())
                .contentType(contentType)
                .maxSizeBytes(filesSizeBytes)
                .build();
    }

}
