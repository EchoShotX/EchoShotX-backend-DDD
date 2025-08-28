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
