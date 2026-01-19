package com.example.echoshotx.notification.presentation.dto.response;

import com.example.echoshotx.notification.domain.entity.Notification;
import com.example.echoshotx.notification.domain.entity.NotificationStatus;
import com.example.echoshotx.notification.domain.entity.NotificationType;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 알림(Notification) 정보를 클라이언트로 반환하기 위한 응답 DTO.
 *
 * <p>알림의 타입, 제목, 내용, 읽음 여부, 상태, 관련된 비디오/크레딧 정보 등을 포함한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String category;
    private String title;
    private String content;
    private Boolean isRead;
    private NotificationStatus status;
    private Integer retryCount;
    private Long videoId;
    private Long creditHistoryId;
    private LocalDateTime createdAt;

    /**
     * Notification 엔티티로부터 Response DTO를 생성한다.
     *
     * @param notification 알림 엔티티
     * @return NotificationResponse 변환된 응답 DTO
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .category(notification.getType().getCategory())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .status(notification.getStatus())
                .retryCount(notification.getRetryCount())
                .videoId(notification.getVideoId())
                .creditHistoryId(notification.getCreditHistoryId())
                .createdAt(notification.getCreatedDate())
                .build();
    }

    public static NotificationResponse success(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .category(notification.getType().getCategory())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .status(NotificationStatus.SENT)
                .retryCount(notification.getRetryCount())
                .videoId(notification.getVideoId())
                .creditHistoryId(notification.getCreditHistoryId())
                .createdAt(notification.getCreatedDate())
                .build();
    }

}
