package com.example.echoshotx.domain.video.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 영상의 모든 URL 정보를 담는 Value Object
 * 
 * 주요 특징:
 * 1. 불변 객체 (Immutable)
 * 2. 도메인 개념을 명확하게 표현
 * 3. URL 생성 실패 시에도 안전하게 처리
 * 4. JPA Embeddable로 Video 엔티티에 직접 임베드
 */
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
    
    /**
     * 썸네일 URL이 유효한지 확인
     */
    public boolean hasThumbnailUrl() {
        return thumbnailUrl != null;
    }
    
    /**
     * 스트리밍 URL이 유효한지 확인
     */
    public boolean hasStreamingUrl() {
        return streamingUrl != null;
    }
    
    /**
     * 다운로드 URL이 유효한지 확인
     */
    public boolean hasDownloadUrl() {
        return downloadUrl != null;
    }
}
