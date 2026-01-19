package com.example.echoshotx.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 테스트 알림 발송 요청 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "테스트 알림 발송 요청")
public class TestNotificationRequest {

    @NotNull(message = "대상 회원 ID는 필수입니다")
    @Schema(description = "알림을 받을 회원 ID", example = "1")
    private Long targetMemberId;

    @Schema(description = "알림 제목 (미입력 시 기본값 사용)", example = "테스트 알림")
    private String title;

    @Schema(description = "알림 내용 (미입력 시 기본값 사용)", example = "SSE 연결 테스트 알림입니다.")
    private String content;
}
