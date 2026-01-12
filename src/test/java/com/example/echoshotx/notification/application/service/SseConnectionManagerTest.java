package com.example.echoshotx.notification.application.service;

import static org.assertj.core.api.Assertions.*;

import com.example.echoshotx.notification.domain.entity.NotificationType;
import com.example.echoshotx.notification.presentation.dto.response.NotificationResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SseConnectionManager 단위 테스트.
 *
 * <p>
 * 테스트 범위:
 * <ol>
 * <li>SSE 연결 생성 및 관리</li>
 * <li>단일 디바이스 연결 정책 (새 연결 시 기존 연결 교체)</li>
 * <li>알림 전송(단일/브로드캐스트)</li>
 * <li>연결 상태 관리</li>
 * </ol>
 */
@DisplayName("SseConnectionManager 테스트")
class SseConnectionManagerTest {

	private SseConnectionManager sseConnectionManager;
	private Long testMemberId1;
	private Long testMemberId2;

	@BeforeEach
	void setUp() {
		sseConnectionManager = new SseConnectionManager();
		testMemberId1 = 1L;
		testMemberId2 = 2L;
	}

	@Nested
	@DisplayName("SSE 연결 생성 테스트")
	class CreateConnectionTest {

		@Test
		@DisplayName("성공: 새로운 SSE 연결 생성")
		void createConnection_Success() {
			// When
			SseEmitter emitter = sseConnectionManager.createConnection(testMemberId1);

			// Then
			assertThat(emitter).isNotNull();
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();
			assertThat(sseConnectionManager.getConnectionCount(testMemberId1)).isEqualTo(1);
		}

		@Test
		@DisplayName("성공: 같은 회원이 재연결하면 기존 연결 교체 (단일 디바이스 정책)")
		void createConnection_ReplacesExisting_SingleDevicePolicy() {
			// Given
			SseEmitter emitter1 = sseConnectionManager.createConnection(testMemberId1);

			// When
			SseEmitter emitter2 = sseConnectionManager.createConnection(testMemberId1);

			// Then
			assertThat(emitter1).isNotNull();
			assertThat(emitter2).isNotNull();
			assertThat(emitter1).isNotEqualTo(emitter2); // 다른 emitter
			assertThat(sseConnectionManager.getConnectionCount(testMemberId1)).isEqualTo(1); // 여전히 1개
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();
		}

		@Test
		@DisplayName("성공: 여러 회원의 연결 생성")
		void createConnection_MultipleMembers_Success() {
			// When
			SseEmitter emitter1 = sseConnectionManager.createConnection(testMemberId1);
			SseEmitter emitter2 = sseConnectionManager.createConnection(testMemberId2);

			// Then
			assertThat(emitter1).isNotNull();
			assertThat(emitter2).isNotNull();
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();
			assertThat(sseConnectionManager.isConnected(testMemberId2)).isTrue();
			assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("알림 전송 테스트")
	class SendNotificationTest {

		@Test
		@DisplayName("성공: 연결된 회원에게 알림 전송")
		void sendToMember_Success_WhenConnected() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			NotificationResponse testNotification = NotificationResponse.builder()
					.id(1L)
					.type(NotificationType.VIDEO_PROCESSING_STARTED)
					.title("Test")
					.content("Test content")
					.isRead(false)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			boolean result = sseConnectionManager.sendToMember(testMemberId1, testNotification);

			// Then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("실패: 연결되지 않은 회원에게 알림 전송")
		void sendToMember_ReturnsFalse_WhenNotConnected() {
			// Given
			Long disconnectedMemberId = 999L;
			NotificationResponse testNotification = NotificationResponse.builder()
					.id(1L)
					.type(NotificationType.VIDEO_PROCESSING_STARTED)
					.title("Test")
					.content("Test content")
					.isRead(false)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			boolean result = sseConnectionManager.sendToMember(disconnectedMemberId, testNotification);

			// Then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("성공: 여러 회원에게 브로드캐스트")
		void broadcast_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			sseConnectionManager.createConnection(testMemberId2);

			List<Long> targetMembers = List.of(testMemberId1, testMemberId2);
			NotificationResponse testNotification = NotificationResponse.builder()
					.id(1L)
					.type(NotificationType.SYSTEM_ANNOUNCEMENT)
					.title("System Message")
					.content("Test broadcast")
					.isRead(false)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			sseConnectionManager.broadcast(targetMembers, testNotification);

			// Then - 예외 없이 실행되면 성공
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();
			assertThat(sseConnectionManager.isConnected(testMemberId2)).isTrue();
		}

		@Test
		@DisplayName("성공: 모든 연결된 회원에게 브로드캐스트")
		void broadcastToAll_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			sseConnectionManager.createConnection(testMemberId2);

			NotificationResponse testNotification = NotificationResponse.builder()
					.id(1L)
					.type(NotificationType.SYSTEM_ANNOUNCEMENT)
					.title("System Message")
					.content("Test broadcast to all")
					.isRead(false)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			sseConnectionManager.broadcastToAll(testNotification);

			// Then - 예외 없이 실행되고 모든 연결이 유지되면 성공
			assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("연결 상태 관리 테스트")
	class ConnectionManagementTest {

		@Test
		@DisplayName("성공: 특정 회원 연결 해제")
		void disconnectMember_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			assertThat(sseConnectionManager.getConnectionCount(testMemberId1)).isEqualTo(1);

			// When
			sseConnectionManager.disconnectMember(testMemberId1);

			// Then
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isFalse();
			assertThat(sseConnectionManager.getConnectionCount(testMemberId1)).isEqualTo(0);
		}

		@Test
		@DisplayName("성공: 모든 연결 해제")
		void disconnectAll_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			sseConnectionManager.createConnection(testMemberId2);
			assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(2);

			// When
			sseConnectionManager.disconnectAll();

			// Then
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isFalse();
			assertThat(sseConnectionManager.isConnected(testMemberId2)).isFalse();
			assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(0);
		}

		@Test
		@DisplayName("성공: 연결되지 않은 회원 연결 해제 시도")
		void disconnectMember_NoError_WhenNotConnected() {
			// Given
			Long nonExistentMemberId = 999L;

			// When & Then - 예외 없이 실행되면 성공
			assertThatCode(() -> sseConnectionManager.disconnectMember(nonExistentMemberId))
					.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("연결 상태 조회 테스트")
	class ConnectionStatusTest {

		@Test
		@DisplayName("성공: 회원별 연결 수 조회 (단일 디바이스이므로 최대 1)")
		void getConnectionCount_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			sseConnectionManager.createConnection(testMemberId2);

			// When & Then
			assertThat(sseConnectionManager.getConnectionCount(testMemberId1)).isEqualTo(1);
			assertThat(sseConnectionManager.getConnectionCount(testMemberId2)).isEqualTo(1);
		}

		@Test
		@DisplayName("성공: 연결되지 않은 회원의 연결 수는 0")
		void getConnectionCount_ReturnsZero_WhenNotConnected() {
			// Given
			Long disconnectedMemberId = 999L;

			// When & Then
			assertThat(sseConnectionManager.getConnectionCount(disconnectedMemberId)).isEqualTo(0);
		}

		@Test
		@DisplayName("성공: 전체 연결 수 조회")
		void getTotalConnectionCount_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			sseConnectionManager.createConnection(testMemberId2);

			// When & Then
			assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(2);
		}

		@Test
		@DisplayName("성공: 회원 연결 여부 확인")
		void isConnected_Success() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);

			// When & Then
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();
			assertThat(sseConnectionManager.isConnected(testMemberId2)).isFalse();
		}

		@Test
		@DisplayName("성공: 연결 후 해제하면 isConnected는 false")
		void isConnected_ReturnsFalse_AfterDisconnect() {
			// Given
			sseConnectionManager.createConnection(testMemberId1);
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isTrue();

			// When
			sseConnectionManager.disconnectMember(testMemberId1);

			// Then
			assertThat(sseConnectionManager.isConnected(testMemberId1)).isFalse();
		}
	}
}
