package com.example.echoshotx.notification.presentation.controller;

import com.example.echoshotx.notification.application.service.NotificationService;
import com.example.echoshotx.notification.application.service.SseConnectionManager;
import com.example.echoshotx.notification.domain.entity.Notification;
import com.example.echoshotx.notification.domain.entity.NotificationStatus;
import com.example.echoshotx.notification.presentation.dto.request.BroadcastNotificationRequest;
import com.example.echoshotx.notification.presentation.dto.request.TestNotificationRequest;
import com.example.echoshotx.notification.presentation.dto.response.NotificationResponse;
import com.example.echoshotx.notification.presentation.dto.response.SseConnectionStatusResponse;
import com.example.echoshotx.notification.presentation.dto.response.TestNotificationResponse;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 테스트용 알림 API 컨트롤러.
 * 
 * <p>
 * 배포 환경에서 SSE 알림 수신을 테스트하기 위한 관리용 API입니다.
 * 인증 없이 접근 가능하므로 프로덕션 환경에서는 사용에 주의가 필요합니다.
 */
@Slf4j
@Tag(name = "Test Notification", description = "테스트용 알림 API (인증 없음, 배포 테스트용)")
@RestController
@RequestMapping("/test/notifications")
@RequiredArgsConstructor
public class TestNotificationController {

    private final NotificationService notificationService;
    private final SseConnectionManager sseConnectionManager;

    @Operation(summary = "테스트 알림 발송", description = "특정 회원에게 테스트 알림을 발송합니다. "
            + "SSE 연결이 있으면 실시간으로 전송되며, 없으면 DB에만 저장됩니다.")
    @PostMapping("/send")
    public ApiResponseDto<NotificationResponse> sendTestNotification(
            @Valid @RequestBody TestNotificationRequest request) {

        log.info("Test notification request: targetMemberId={}", request.getTargetMemberId());

        Notification notification = notificationService.createAndSendTestNotification(
                request.getTargetMemberId(),
                request.getTitle(),
                request.getContent());

        // 전송 상태에 따른 응답 생성
        NotificationResponse response;
        if (notification.getStatus() == NotificationStatus.SENT) {
            response = NotificationResponse.success(
                    notification);
        } else {
            response = NotificationResponse.success(
                    notification);
        }

        sseConnectionManager.sendToMember(request.getTargetMemberId(), response);

        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "전체 SSE 연결 상태 조회", description = "현재 활성화된 모든 SSE 연결 수를 조회합니다.")
    @GetMapping("/connections")
    public ApiResponseDto<SseConnectionStatusResponse> getConnectionStatus() {
        int totalConnections = sseConnectionManager.getTotalConnectionCount();
        log.info("SSE connection status requested: totalConnections={}", totalConnections);
        return ApiResponseDto.onSuccess(SseConnectionStatusResponse.of(totalConnections));
    }

    @Operation(summary = "특정 회원 SSE 연결 상태 조회", description = "특정 회원의 SSE 연결 여부를 조회합니다.")
    @GetMapping("/connections/{memberId}")
    public ApiResponseDto<Boolean> getMemberConnectionStatus(@PathVariable Long memberId) {
        boolean isConnected = sseConnectionManager.isConnected(memberId);
        log.info("Member SSE connection status: memberId={}, connected={}", memberId, isConnected);
        return ApiResponseDto.onSuccess(isConnected);
    }

    @Operation(summary = "전체 연결된 회원에게 브로드캐스트", description = "현재 SSE로 연결된 모든 회원에게 테스트 알림을 브로드캐스트합니다. "
            + "DB에는 저장하지 않고 실시간 전송만 수행합니다.")
    @PostMapping("/broadcast")
    public ApiResponseDto<String> broadcastNotification(
            @RequestBody BroadcastNotificationRequest request) {

        int totalConnections = sseConnectionManager.getTotalConnectionCount();
        log.info("Broadcast test notification requested: totalConnections={}", totalConnections);

        if (totalConnections == 0) {
            return ApiResponseDto.onSuccess("연결된 SSE 클라이언트가 없습니다.");
        }

        // 실시간 브로드캐스트용 임시 응답 객체 생성
        NotificationResponse broadcastResponse = NotificationResponse.builder()
                .id(0L)
                .type(com.example.echoshotx.notification.domain.entity.NotificationType.TEST_NOTIFICATION)
                .category("테스트")
                .title(request.getTitle() != null ? request.getTitle() : "📢 전체 테스트 알림")
                .content(request.getContent() != null ? request.getContent() : "브로드캐스트 테스트 메시지입니다.")
                .isRead(false)
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        sseConnectionManager.broadcastToAll(broadcastResponse);

        return ApiResponseDto.onSuccess(
                String.format("%d개의 연결에 브로드캐스트를 전송했습니다.", totalConnections));
    }

    @Operation(summary = "🔧 테스트용 SSE 구독 (토큰 불필요)", 
            description = "토큰 없이 memberId를 직접 지정하여 SSE 연결을 테스트합니다. " +
                    "curl -N 'http://서버주소/test/notifications/subscribe/1' 로 테스트 가능합니다.")
    @GetMapping(value = "/subscribe/{memberId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testSubscribe(@PathVariable Long memberId) {
        log.info("🔧 [TEST] SSE connection request for memberId: {}", memberId);
        return sseConnectionManager.createConnection(memberId);
    }
}
