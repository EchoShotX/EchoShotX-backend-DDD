package com.example.echoshotx.notification.application.adaptor;

import com.example.echoshotx.notification.domain.entity.Notification;
import com.example.echoshotx.notification.domain.entity.NotificationStatus;
import com.example.echoshotx.notification.domain.entity.NotificationType;
import com.example.echoshotx.notification.domain.exception.NotificationErrorStatus;
import com.example.echoshotx.notification.infrastructure.persistence.NotificationRepository;
import com.example.echoshotx.notification.presentation.exception.NotificationHandler;
import com.example.echoshotx.shared.annotation.adaptor.Adaptor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 조회/저장에 대한 어댑터 계층.
 *
 * <p>영속성 레이어(repository)에 직접 접근하지 않고, 도메인 서비스/유스케이스가 이 어댑터를 통해
 * 알림을 조회/저장하도록 한다.
 */
@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationAdaptor {

  private final NotificationRepository notificationRepository;

  /** ID로 알림을 조회한다. 존재하지 않으면 NOTIFICATION_NOT_FOUND 예외를 던진다. */
  public Notification queryById(Long notificationId) {
	return notificationRepository
		.findById(notificationId)
		.orElseThrow(
			() -> new NotificationHandler(NotificationErrorStatus.NOTIFICATION_NOT_FOUND));
  }

  /** 회원의 모든 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryAllByMemberId(Long memberId) {
	return notificationRepository.findByMemberIdOrderByCreatedDateDesc(memberId);
  }

  /** 회원의 읽지 않은 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryUnreadByMemberId(Long memberId) {
	return notificationRepository.findByMemberIdAndIsReadOrderByCreatedDateDesc(memberId, false);
  }

  /** 회원의 읽은 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryReadByMemberId(Long memberId) {
	return notificationRepository.findByMemberIdAndIsReadOrderByCreatedDateDesc(memberId, true);
  }

  /** 회원과 타입으로 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryByMemberIdAndType(Long memberId, NotificationType type) {
	return notificationRepository.findByMemberIdAndTypeOrderByCreatedDateDesc(memberId, type);
  }

  /** 회원과 타입 기준의 읽지 않은 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryUnreadByMemberIdAndType(Long memberId, NotificationType type) {
	return notificationRepository.findByMemberIdAndIsReadAndTypeOrderByCreatedDateDesc(
		memberId, false, type);
  }

  /** 회원의 읽지 않은 알림 개수를 반환한다. */
  public Long countUnreadByMemberId(Long memberId) {
	return notificationRepository.countByMemberIdAndIsRead(memberId, false);
  }

  /**
   * 재시도 대상 FAILED 알림을 조회한다.
   *
   * @param retryAfter 해당 시각 이후 재시도 가능한 알림만 조회
   */
  public List<Notification> queryFailedNotificationsForRetry(LocalDateTime retryAfter) {
	return notificationRepository.findFailedNotificationsForRetry(
		NotificationStatus.FAILED, retryAfter);
  }

  /** 상태값으로 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryByStatus(NotificationStatus status) {
	return notificationRepository.findByStatusOrderByCreatedDateDesc(status);
  }

  /** 비디오 ID로 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryByVideoId(Long videoId) {
	return notificationRepository.findByVideoIdOrderByCreatedDateDesc(videoId);
  }

  /** 크레딧 이력 ID로 알림을 생성일시 내림차순으로 조회한다. */
  public List<Notification> queryByCreditHistoryId(Long creditHistoryId) {
	return notificationRepository.findByCreditHistoryIdOrderByCreatedDateDesc(creditHistoryId);
  }

  /**
   * 알림 소유권을 검증한다.
   *
   * <p>알림이 해당 회원 소유가 아니면 NOTIFICATION_ACCESS_DENIED 예외를 던진다.
   */
  public void validateNotificationOwnership(Long notificationId, Long memberId) {
	Notification notification = queryById(notificationId);
	if (!notification.getMemberId().equals(memberId)) {
	  throw new NotificationHandler(NotificationErrorStatus.NOTIFICATION_ACCESS_DENIED);
	}
  }

  /** 알림을 저장한다. */
  @Transactional
  public Notification save(Notification notification) {
	return notificationRepository.save(notification);
  }

  /** 여러 알림을 일괄 저장한다. */
  @Transactional
  public List<Notification> saveAll(List<Notification> notifications) {
	return notificationRepository.saveAll(notifications);
  }

  /** ID로 알림을 삭제한다. */
  @Transactional
  public void delete(Long notificationId) {
	notificationRepository.deleteById(notificationId);
  }

  /**
   * 기준 일시 이전의 알림을 정리(삭제)한다.
   *
   * @param cutoffDate 해당 일시 이전의 데이터 삭제
   */
  @Transactional
  public void deleteOldNotifications(LocalDateTime cutoffDate) {
	notificationRepository.deleteByCreatedDateBefore(cutoffDate);
  }
}
