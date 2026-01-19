package com.example.echoshotx.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 테스트 알림 발송 결과 응답 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "테스트 알림 발송 결과")
public class TestNotificationResponse {

    @Schema(description = "생성된 알림 ID", example = "123")
    private Long notificationId;

    @Schema(description = "대상 회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "SSE로 실시간 전송 성공 여부", example = "true")
    private Boolean delivered;

    @Schema(description = "결과 메시지", example = "알림이 성공적으로 전송되었습니다.")
    private String message;

    public static TestNotificationResponse success(Long notificationId, Long memberId) {
        return TestNotificationResponse.builder()
                .notificationId(notificationId)
                .memberId(memberId)
                .delivered(true)
                .message("알림이 성공적으로 전송되었습니다.")
                .build();
    }

    public static TestNotificationResponse pending(Long notificationId, Long memberId) {
        return TestNotificationResponse.builder()
                .notificationId(notificationId)
                .memberId(memberId)
                .delivered(false)
                .message("알림이 생성되었지만 SSE 연결이 없어 실시간 전송되지 않았습니다.")
                .build();
    }
}
