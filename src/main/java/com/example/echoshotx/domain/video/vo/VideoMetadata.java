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

}
