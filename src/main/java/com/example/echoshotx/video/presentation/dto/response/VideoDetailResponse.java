package com.example.echoshotx.video.presentation.dto.response;

import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
//todo 메타데이터 비교 정보
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
                .originalFileName(video.getOriginalFile().getFileName())
                .s3OriginalKey(video.getOriginalFile().getS3Key())
                .s3ProcessedKey(video.getProcessedVideo() != null ? video.getProcessedVideo().getS3Key() : null)
                .s3ThumbnailKey(video.getS3ThumbnailKey())
                .fileSizeBytes(video.getProcessedVideo() != null ? video.getProcessedVideo().getFileSizeBytes() : null)
                .status(video.getStatus())
                .processingType(video.getProcessingType())
                .metadata(video.getProcessedMetadata())
                .uploadedAt(video.getCreatedDate())
                .updatedAt(video.getLastModifiedDate())
                .build();
    }
}
