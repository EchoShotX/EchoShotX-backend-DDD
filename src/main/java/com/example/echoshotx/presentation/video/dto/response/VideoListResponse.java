package com.example.echoshotx.presentation.video.dto.response;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VideoListResponse {
    
    private Long videoId;
    private String originalFileName;
    private Long fileSizeBytes;
    private VideoStatus status;
    private ProcessingType processingType;
    private String s3ThumbnailKey;  // 썸네일 경로 (목록에서 표시용)
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    
    public static VideoListResponse from(Video video) {
        return VideoListResponse.builder()
                .videoId(video.getId())
                .originalFileName(video.getOriginalFileName())
                .fileSizeBytes(video.getFileSizeBytes())
                .status(video.getStatus())
                .processingType(video.getProcessingType())
                .s3ThumbnailKey(video.getS3ThumbnailKey())
                .uploadedAt(video.getCreatedDate())
                .updatedAt(video.getLastModifiedDate())
                .build();
    }
}
