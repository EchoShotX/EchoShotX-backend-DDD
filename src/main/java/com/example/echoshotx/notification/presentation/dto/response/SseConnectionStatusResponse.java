package com.example.echoshotx.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 연결 상태 응답 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "SSE 연결 상태 정보")
public class SseConnectionStatusResponse {

    @Schema(description = "전체 활성 SSE 연결 수", example = "15")
    private Integer totalConnections;

    @Schema(description = "조회 시각")
    private LocalDateTime timestamp;

    public static SseConnectionStatusResponse of(int totalConnections) {
        return SseConnectionStatusResponse.builder()
                .totalConnections(totalConnections)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
