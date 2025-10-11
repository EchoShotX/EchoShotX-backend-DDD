package com.example.echoshotx.domain.video.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VideoUrls {
    
    private String thumbnailUrl;     // 썸네일 Pre-signed URL
    private String streamingUrl;     // 스트리밍 Pre-signed URL
    private String downloadUrl;      // 다운로드 Pre-signed URL
    private LocalDateTime expiresAt; // URL 만료 시간
    
    // URL 생성 실패 시 사용할 기본값
    public static VideoUrls empty() {
        return VideoUrls.builder()
                .thumbnailUrl(null)
                .streamingUrl(null)
                .downloadUrl(null)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
    
    /**
     * 모든 URL이 유효한지 확인
     */
    public boolean hasValidUrls() {
        return thumbnailUrl != null || streamingUrl != null || downloadUrl != null;
    }

    public VideoUrls updateThumbnailUrl(String thumbnailUrl) {
        return VideoUrls.builder()
                .thumbnailUrl(thumbnailUrl)
                .streamingUrl(this.streamingUrl)
                .downloadUrl(this.downloadUrl)
                .expiresAt(this.expiresAt)
                .build();
    }

    public VideoUrls updateStreamingUrl(String streamingUrl) {
        return VideoUrls.builder()
                .thumbnailUrl(this.thumbnailUrl)
                .streamingUrl(streamingUrl)
                .downloadUrl(this.downloadUrl)
                .expiresAt(this.expiresAt)
                .build();
    }
}
