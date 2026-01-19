package com.example.echoshotx.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 브로드캐스트 알림 요청 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "브로드캐스트 알림 요청")
public class BroadcastNotificationRequest {

    @Schema(description = "알림 제목 (미입력 시 기본값 사용)", example = "전체 공지")
    private String title;

    @Schema(description = "알림 내용 (미입력 시 기본값 사용)", example = "전체 테스트 메시지입니다.")
    private String content;
}
