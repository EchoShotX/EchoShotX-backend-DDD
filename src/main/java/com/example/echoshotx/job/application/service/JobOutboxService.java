package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.domain.entity.JobOutboxEvent;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.persistence.JobOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobOutboxService {

    static final int MAX_RETRIES = 10;

    private final JobOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueJobCreated(Job job, Long memberId) {
        JobMessage message = JobMessage.builder()
                .jobId(job.getId())
                .videoId(job.getVideoId())
                .processingType(job.getProcessingType().name())
                .memberId(memberId)
                .s3Key(job.getS3Key())
                .build();

        repository.save(JobOutboxEvent.pending(job.getId(), serialize(message), LocalDateTime.now()));
    }

    @Transactional
    public void markSent(Long eventId) {
        repository.findById(eventId).ifPresent(event -> event.markSent(LocalDateTime.now()));
    }

    /**
     * 발송 실패 처리. 재시도 횟수가 MAX_RETRIES 이상이면 FAILED로 확정하고 true 반환.
     * 그 이하면 exponential backoff 후 재시도 예약하고 false 반환.
     */
    @Transactional
    public boolean markRetryOrFailed(Long eventId, RuntimeException error) {
        return repository.findById(eventId).map(event -> {
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.markFailed(LocalDateTime.now(), error);
                log.error("Outbox event permanently failed. eventId={}", eventId, error);
                return true;
            }
            event.markRetry(LocalDateTime.now(), error);
            log.warn("Outbox event will retry. eventId={}, retryCount={}", eventId, event.getRetryCount(), error);
            return false;
        }).orElse(false);
    }

    public JobMessage deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, JobMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox payload", e);
        }
    }

    private String serialize(JobMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
