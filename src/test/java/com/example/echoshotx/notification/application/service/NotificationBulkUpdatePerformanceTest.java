package com.example.echoshotx.notification.application.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.example.echoshotx.notification.domain.entity.Notification;
import com.example.echoshotx.notification.domain.entity.NotificationStatus;
import com.example.echoshotx.notification.domain.entity.NotificationType;
import com.example.echoshotx.notification.infrastructure.persistence.NotificationRepository;

/**
 * 알림 전체 읽음 처리 성능 비교 테스트.
 *
 * <p>
 * N+1 UPDATE vs Bulk Update 실제 성능 측정
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("알림 전체 읽음 처리 성능 비교 테스트")
class NotificationBulkUpdatePerformanceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    private static final Long TEST_MEMBER_ID = 999999L;
    private static final int NOTIFICATION_COUNT = 1000;

    @Nested
    @DisplayName("성능 비교 벤치마크")
    class PerformanceBenchmark {

        @Test
        @DisplayName("📊 Before vs After: 1000개 알림 전체 읽음 처리 성능 비교")
        void bulkUpdate_PerformanceComparison() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 알림 전체 읽음 처리 성능 비교 테스트 (N=" + NOTIFICATION_COUNT + ")");
            System.out.println("=".repeat(60) + "\n");

            // === 1. 테스트 데이터 생성 ===
            System.out.println("[1] 테스트 데이터 생성 중...");
            long setupStart = System.currentTimeMillis();

            List<Notification> notifications = new ArrayList<>();
            for (int i = 0; i < NOTIFICATION_COUNT; i++) {
                notifications.add(Notification.builder()
                        .memberId(TEST_MEMBER_ID)
                        .type(NotificationType.VIDEO_PROCESSING_COMPLETED)
                        .title("테스트 알림 " + i)
                        .content("테스트 내용 " + i)
                        .isRead(false)
                        .status(NotificationStatus.SENT)
                        .retryCount(0)
                        .build());
            }
            notificationRepository.saveAll(notifications);
            notificationRepository.flush();

            long setupTime = System.currentTimeMillis() - setupStart;
            System.out.println("→ " + NOTIFICATION_COUNT + "개 알림 생성 완료 (" + setupTime + "ms)");
            System.out.println();

            // === 2. Before 시뮬레이션 (N+1 방식) ===
            System.out.println("[2] Before (N+1 방식) 시뮬레이션");
            System.out
                    .println("→ SELECT 1개 + UPDATE " + NOTIFICATION_COUNT + "개 = " + (NOTIFICATION_COUNT + 1) + "개 쿼리");

            // 데이터 초기화 (읽지 않음 상태로)
            resetNotificationsToUnread();

            long beforeStart = System.currentTimeMillis();

            // N+1 방식 시뮬레이션: 개별 조회 후 개별 저장
            List<Notification> unreadList = notificationRepository
                    .findByMemberIdAndIsReadOrderByCreatedDateDesc(TEST_MEMBER_ID, false);
            for (Notification n : unreadList) {
                n.markAsRead();
            }
            notificationRepository.saveAll(unreadList);
            notificationRepository.flush();

            long beforeTime = System.currentTimeMillis() - beforeStart;
            System.out.println("→ 실행 시간: " + beforeTime + "ms");
            System.out.println();

            // === 3. After (Bulk Update 방식) ===
            System.out.println("[3] After (Bulk Update 방식)");
            System.out.println("→ UPDATE 1개 쿼리");

            // 데이터 초기화
            resetNotificationsToUnread();

            long afterStart = System.currentTimeMillis();

            // Bulk Update 방식
            notificationService.markAllAsRead(TEST_MEMBER_ID);

            long afterTime = System.currentTimeMillis() - afterStart;
            System.out.println("→ 실행 시간: " + afterTime + "ms");
            System.out.println();

            // === 4. 결과 비교 ===
            double improvement = beforeTime > 0 ? ((double) (beforeTime - afterTime) / beforeTime) * 100 : 0;
            double speedup = afterTime > 0 ? (double) beforeTime / afterTime : 0;

            System.out.println("=".repeat(60));
            System.out.println("📈 성능 비교 결과");
            System.out.println("=".repeat(60));
            System.out.println();
            System.out.println("| 지표           | Before (N+1) | After (Bulk) | 개선     |");
            System.out.println("|----------------|--------------|--------------|----------|");
            System.out.printf("| 쿼리 수        | %d개%s | 1개%s | 99.9%% ↓ |%n",
                    NOTIFICATION_COUNT + 1,
                    " ".repeat(Math.max(0, 6 - String.valueOf(NOTIFICATION_COUNT + 1).length())),
                    " ".repeat(7));
            System.out.printf("| 실행 시간      | %dms%s | %dms%s | %.1f%% ↓  |%n",
                    beforeTime, " ".repeat(Math.max(0, 8 - String.valueOf(beforeTime).length())),
                    afterTime, " ".repeat(Math.max(0, 8 - String.valueOf(afterTime).length())),
                    improvement);
            System.out.printf("| 속도 향상      | 기준         | %.1fx 빠름%s |          |%n",
                    speedup, " ".repeat(Math.max(0, 5 - String.valueOf(String.format("%.1f", speedup)).length())));
            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println();

            // === 5. Assertions ===
            // Bulk Update가 N+1 방식보다 빨라야 함
            assertThat(afterTime).isLessThan(beforeTime);

            // 모든 알림이 읽음 처리되었는지 확인
            long unreadCount = notificationRepository.countByMemberIdAndIsRead(TEST_MEMBER_ID, false);
            assertThat(unreadCount).isZero();

            System.out.println("✅ 테스트 통과! Bulk Update가 " + String.format("%.1fx", speedup) + " 빠름");
        }

        private void resetNotificationsToUnread() {
            // JPQL로 모든 알림을 읽지 않음 상태로 초기화
            notificationRepository.bulkMarkAsReadByMemberId(TEST_MEMBER_ID); // 먼저 읽음 처리
            // 다시 읽지 않음으로 변경 (직접 쿼리로)
            List<Notification> all = notificationRepository
                    .findByMemberIdOrderByCreatedDateDesc(TEST_MEMBER_ID);
            for (Notification n : all) {
                // Reflection으로 isRead 필드 변경 (private 필드이므로)
                try {
                    java.lang.reflect.Field isReadField = Notification.class.getDeclaredField("isRead");
                    isReadField.setAccessible(true);
                    isReadField.set(n, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            notificationRepository.saveAll(all);
            notificationRepository.flush();
        }
    }
}
