package com.example.echoshotx.notification.infrastructure.persistence;

import com.example.echoshotx.notification.domain.entity.Notification;
import com.example.echoshotx.notification.domain.entity.NotificationStatus;
import com.example.echoshotx.notification.domain.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 알림(Notification) 엔티티에 대한 JPA Repository.
 *
 * <p>회원 알림 조회, 읽음 여부 필터링, 타입별 조회, 상태 기반 조회,
 * 재시도 가능한 알림 검색 등 다양한 쿼리 메서드를 제공한다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /** 특정 회원의 전체 알림을 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByMemberIdOrderByCreatedDateDesc(Long memberId);

  /** 특정 회원의 읽지 않은 알림을 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByMemberIdAndIsReadOrderByCreatedDateDesc(Long memberId, Boolean isRead);

  /** 특정 회원과 알림 타입으로 필터링하여 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByMemberIdAndTypeOrderByCreatedDateDesc(Long memberId, NotificationType type);

  /** 회원, 읽음 여부, 알림 타입을 조건으로 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByMemberIdAndIsReadAndTypeOrderByCreatedDateDesc(
	  Long memberId, Boolean isRead, NotificationType type
  );

  /** 특정 회원의 읽지 않은 알림 개수를 반환한다. */
  Long countByMemberIdAndIsRead(Long memberId, Boolean isRead);

  /**
   * 재시도 가능한 FAILED 알림 목록을 조회한다.
   *
   * <p>조건:
   * <ul>
   *   <li>상태: FAILED</li>
   *   <li>retryCount &lt; 3</li>
   *   <li>lastRetryAt이 null이거나, 지정된 retryAfter 이전</li>
   * </ul>
   */
  @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < 3 "
	  + "AND (n.lastRetryAt IS NULL OR n.lastRetryAt < :retryAfter)")
  List<Notification> findFailedNotificationsForRetry(
	  @Param("status") NotificationStatus status,
	  @Param("retryAfter") LocalDateTime retryAfter
  );

  /** 알림 상태값으로 필터링하여 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByStatusOrderByCreatedDateDesc(NotificationStatus status);

  /** 특정 시점 이전에 생성된 알림을 삭제한다. (정리 용도) */
  void deleteByCreatedDateBefore(LocalDateTime date);

  /** 해당 비디오 ID에 대한 알림을 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByVideoIdOrderByCreatedDateDesc(Long videoId);

  /** 특정 크레딧 히스토리 ID에 연결된 알림을 생성일 기준 내림차순으로 조회한다. */
  List<Notification> findByCreditHistoryIdOrderByCreatedDateDesc(Long creditHistoryId);

  /** 특정 회원의 모든 미읽음 알림을 일괄 읽음 처리함 **/
  @Modifying(clearAutomatically = true) // bulk update 후 영속성 컨텍스트 자동으로 clear
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.memberId = :memberId AND n.isRead = false")
  int bulkMarkAsReadByMemberId(@Param("memberId") Long memberId);

}
