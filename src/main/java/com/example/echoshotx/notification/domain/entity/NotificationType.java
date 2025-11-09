package com.example.echoshotx.notification.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 유형.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

  VIDEO_PROCESSING_STARTED("영상", "영상 처리가 시작되었습니다"),
  VIDEO_PROCESSING_COMPLETED("영상", "영상 처리가 완료되었습니다"),
  VIDEO_PROCESSING_FAILED("영상", "영상 처리가 실패했습니다"),

  SYSTEM_ANNOUNCEMENT("시스템", "시스템 공지사항");

  private final String category;
  private final String defaultMessage;
}
