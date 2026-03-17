package com.example.echoshotx.shared.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "webhook_nonce_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_webhook_nonce", columnNames = "nonce")
        })
public class WebhookNonceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nonce", nullable = false, length = 120)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static WebhookNonceRecord create(String nonce, LocalDateTime now, LocalDateTime expiresAt) {
        return WebhookNonceRecord.builder()
                .nonce(nonce)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();
    }
}
