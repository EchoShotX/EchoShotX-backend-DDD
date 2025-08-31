package com.example.echoshotx.presentation.video.dto.response;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VideoUploadResponse {
    
    private Long videoId;
    private String originalFileName;
    private Long fileSizeBytes;
    private VideoStatus status;
    private ProcessingType processingType;
    private LocalDateTime uploadedAt;
    
    public static VideoUploadResponse from(Video video) {
        return VideoUploadResponse.builder()
                .videoId(video.getId())
                .originalFileName(video.getOriginalFileName())
                .fileSizeBytes(video.getFileSizeBytes())
                .status(video.getStatus())
                .processingType(video.getProcessingType())
                .uploadedAt(video.getCreatedDate())
                .build();
    }
}
