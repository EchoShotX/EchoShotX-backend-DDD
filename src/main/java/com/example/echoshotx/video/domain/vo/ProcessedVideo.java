package com.example.echoshotx.video.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedVideo {

    private String s3Key;
    private Long fileSizeBytes;

    public boolean exists() {
        return s3Key != null;
    }

    public static ProcessedVideo empty() {
        return ProcessedVideo.builder().build();
    }
}
