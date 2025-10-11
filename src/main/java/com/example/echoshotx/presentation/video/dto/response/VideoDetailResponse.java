package com.example.echoshotx.presentation.video.dto.response;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VideoDetailResponse {
    
    private Long videoId;
    private String originalFileName;
    private String s3OriginalKey;
    private String s3ProcessedKey;
    private String s3ThumbnailKey;
    private Long fileSizeBytes;
    private VideoStatus status;
    private ProcessingType processingType;
    private VideoMetadata metadata;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    
    // 스트리밍 및 다운로드를 위한 URL들
    private String streamingUrl;        // 스트리밍용 Pre-signed URL
    private String downloadUrl;         // 다운로드용 Pre-signed URL
    private String thumbnailUrl;        // 썸네일용 Pre-signed URL
    private LocalDateTime urlExpiresAt; // URL 만료 시간
    
    public static VideoDetailResponse from(Video video) {
        return VideoDetailResponse.builder()
                .videoId(video.getId())
                .originalFileName(video.getOriginalFileName())
                .s3OriginalKey(video.getS3OriginalKey())
                .s3ProcessedKey(video.getS3ProcessedKey())
                .s3ThumbnailKey(video.getS3ThumbnailKey())
                .fileSizeBytes(video.getFileSizeBytes())
                .status(video.getStatus())
                .processingType(video.getProcessingType())
                .metadata(video.getMetadata())
                .uploadedAt(video.getCreatedDate())
                .updatedAt(video.getLastModifiedDate())
                .build();
    }
}
