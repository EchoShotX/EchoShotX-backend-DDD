package com.example.echoshotx.domain.video.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VideoMetadata {
    private Double durationSeconds;    // 영상 길이 (초)
    private Integer width;             // 가로 해상도
    private Integer height;            // 세로 해상도
    private String codec;              // 비디오 코덱
    private Long bitrate;              // 비트레이트
    private Double frameRate;          // 프레임 레이트

    // ========================================
    // 테스트 전용 팩토리 메서드들
    // ========================================

    /**
     * 테스트용 기본 VideoMetadata 인스턴스를 생성합니다
     * 
     *   주의: 이 메서드는 테스트 목적으로만 사용해야 합니다
     * - JPA 엔티티의 protected 생성자를 우회합니다
     * - 운영 코드에서는 절대 사용하지 마세요
     * 
     * @return 테스트용 VideoMetadata 인스턴스 (기본값)
     */
    public static VideoMetadata createForTest() {
        return new VideoMetadata(
                120.0,      // 2분 영상
                1280,       // HD 가로 해상도
                720,        // HD 세로 해상도
                "h264",     // 표준 코덱
                2500000L,   // 2.5Mbps 비트레이트
                30.0        // 30fps
        );
    }

    /**
     * 테스트용 상세 VideoMetadata 인스턴스를 생성합니다
     * 
     * @param durationSeconds 영상 길이 (초)
     * @param width 가로 해상도
     * @param height 세로 해상도
     * @param codec 비디오 코덱
     * @param bitrate 비트레이트
     * @param frameRate 프레임 레이트
     * @return 테스트용 VideoMetadata 인스턴스
     */
    public static VideoMetadata createDetailedForTest(Double durationSeconds, Integer width, Integer height, 
                                                    String codec, Long bitrate, Double frameRate) {
        return new VideoMetadata(durationSeconds, width, height, codec, bitrate, frameRate);
    }

    /**
     * 테스트용 빈 VideoMetadata 인스턴스를 생성합니다 (업로드 직후 상태)
     * 
     * @return 테스트용 VideoMetadata 인스턴스 (모든 값이 null)
     */
    public static VideoMetadata createEmptyForTest() {
        return new VideoMetadata(null, null, null, null, null, null);
    }

    /**
     * 테스트용 고화질 VideoMetadata 인스턴스를 생성합니다
     * 
     * @return 테스트용 VideoMetadata 인스턴스 (4K 고화질)
     */
    public static VideoMetadata createHighQualityForTest() {
        return new VideoMetadata(
                300.5,      // 5분 영상
                3840,       // 4K 가로 해상도
                2160,       // 4K 세로 해상도
                "h265",     // 고효율 코덱
                15000000L,  // 15Mbps 비트레이트
                60.0        // 60fps
        );
    }

    /**
     * 테스트용 저화질 VideoMetadata 인스턴스를 생성합니다
     * 
     * @return 테스트용 VideoMetadata 인스턴스 (저화질)
     */
    public static VideoMetadata createLowQualityForTest() {
        return new VideoMetadata(
                60.0,       // 1분 영상
                640,        // 저해상도 가로
                480,        // 저해상도 세로
                "h264",     // 표준 코덱
                800000L,    // 800kbps 비트레이트
                24.0        // 24fps
        );
    }

}
