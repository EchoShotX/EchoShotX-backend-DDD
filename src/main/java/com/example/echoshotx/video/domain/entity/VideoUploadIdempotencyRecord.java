package com.example.echoshotx.video.domain.entity;

import com.example.echoshotx.shared.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "video_upload_idempotency_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_video_upload_idempotency_key",
                        columnNames = {"member_id", "video_id", "idempotency_key"})
        })
public class VideoUploadIdempotencyRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Lob
    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static VideoUploadIdempotencyRecord create(
            Long memberId,
            Long videoId,
            String idempotencyKey,
            String requestHash,
            Integer responseStatus,
            String responseBody,
            LocalDateTime expiresAt) {
        return VideoUploadIdempotencyRecord.builder()
                .memberId(memberId)
                .videoId(videoId)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }
}
