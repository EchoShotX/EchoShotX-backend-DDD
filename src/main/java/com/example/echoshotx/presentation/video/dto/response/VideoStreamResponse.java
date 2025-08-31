package com.example.echoshotx.presentation.video.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 영상 스트리밍을 위한 Response DTO
 * Pre-signed URL과 만료 시간을 포함하여 클라이언트가 영상을 재생할 수 있도록 함
 */
@Getter
@Builder
public class VideoStreamResponse {
    
    private String videoId;
    private String streamingUrl;        // 스트리밍용 Pre-signed URL
    private String downloadUrl;         // 다운로드용 Pre-signed URL (선택적)
    private String thumbnailUrl;        // 썸네일용 Pre-signed URL
    private LocalDateTime expiresAt;    // URL 만료 시간
    private String contentType;         // 영상 MIME 타입
    private Long fileSizeBytes;         // 파일 크기
    private String fileName;            // 원본 파일명
    
    // 스트리밍 관련 메타데이터
    private Boolean supportsRangeRequests;  // Range 요청 지원 여부
    private String quality;                 // 영상 품질 정보
    private Integer bitrate;                // 비트레이트 (kbps)
}
