package com.example.echoshotx.notification.domain.entity;

import com.example.echoshotx.shared.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 알림 엔티티.
 */
@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notification_member_id", columnList = "member_id"),
    @Index(name = "idx_notification_is_read", columnList = "is_read"),
    @Index(name = "idx_notification_created_date", columnList = "created_date"),
    @Index(name = "idx_notification_member_read", columnList = "member_id, is_read"),
    @Index(name = "idx_notification_status", columnList = "status")
})
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isRead = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(nullable = false)
  @Builder.Default
  private Integer retryCount = 0;

  @Column
  private LocalDateTime lastRetryAt;

  @Column
  private Long videoId;

  @Column
  private Long creditHistoryId;

  /**
   * 비디오 관련 알림 생성 팩토리.
   */
  public static Notification createVideoNotification(
      Long memberId, Long videoId, NotificationType type, String title, String content) {
    validateVideoNotificationType(type);
    return Notification.builder()
        .memberId(memberId)
        .videoId(videoId)
        .type(type)
        .title(title)
        .content(content)
        .isRead(false)
        .status(NotificationStatus.PENDING)
        .retryCount(0)
        .build();
  }

  /**
   * 시스템 알림 생성 팩토리.
   */
  public static Notification createSystemNotification(
      Long memberId, String title, String content) {
    return Notification.builder()
        .memberId(memberId)
        .type(NotificationType.SYSTEM_ANNOUNCEMENT)
        .title(title)
        .content(content)
        .isRead(false)
        .status(NotificationStatus.PENDING)
        .retryCount(0)
        .build();
  }

  /**
   * 테스트 알림 생성 팩토리 (관리자용).
   */
  public static Notification createTestNotification(
      Long memberId, String title, String content) {
    return Notification.builder()
        .memberId(memberId)
        .type(NotificationType.TEST_NOTIFICATION)
        .title(title != null ? title : "🧪 테스트 알림")
        .content(content != null ? content : "SSE 연결 테스트용 알림입니다.")
        .isRead(false)
        .status(NotificationStatus.PENDING)
        .retryCount(0)
        .build();
  }

  /**
   * 읽음 처리.
   */
  public void markAsRead() {
    if (this.isRead) {
      return;
    }
    this.isRead = true;
  }

  /**
   * 전송 완료 처리.
   */
  public void markAsSent() {
    this.status = NotificationStatus.SENT;
  }

  /**
   * 전송 실패 처리 및 재시도 카운트 증가.
   */
  public void markAsFailed() {
    this.status = NotificationStatus.FAILED;
    this.retryCount++;
    this.lastRetryAt = LocalDateTime.now();
  }

  /**
   * 재시도 가능 여부(최대 3회).
   */
  public boolean canRetry() {
    return this.status == NotificationStatus.FAILED && this.retryCount < 3;
  }

  /**
   * 재시도 위한 상태 리셋.
   */
  public void resetForRetry() {
    if (!canRetry()) {
      throw new IllegalStateException(
          "Cannot retry notification - max retry count exceeded or wrong status");
    }
    this.status = NotificationStatus.PENDING;
  }

  private static void validateVideoNotificationType(NotificationType type) {
    if (type != NotificationType.VIDEO_PROCESSING_STARTED
        && type != NotificationType.VIDEO_PROCESSING_COMPLETED
        && type != NotificationType.VIDEO_PROCESSING_FAILED) {
      throw new IllegalArgumentException("Invalid video notification type: " + type);
    }
  }
}
