package com.example.echoshotx.job.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "job_outbox_event")
public class JobOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "payload", nullable = false, length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobOutboxStatus status;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static JobOutboxEvent pending(Long jobId, String payload, LocalDateTime now) {
        return JobOutboxEvent.builder()
                .jobId(jobId)
                .payload(payload)
                .status(JobOutboxStatus.PENDING)
                .retryCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
    }

    public void markSent(LocalDateTime now) {
        this.status = JobOutboxStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    public void markRetry(LocalDateTime now, Exception e) {
        this.retryCount += 1;
        this.nextAttemptAt = now.plusSeconds(Math.min(60L, 1L << Math.min(this.retryCount, 6)));
        this.lastError = e.getMessage();
    }

    public void markFailed(LocalDateTime now, Exception e) {
        this.status = JobOutboxStatus.FAILED;
        this.nextAttemptAt = now;
        this.lastError = e.getMessage();
    }
}
