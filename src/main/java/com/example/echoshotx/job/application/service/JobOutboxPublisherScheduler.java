package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.domain.entity.JobOutboxEvent;
import com.example.echoshotx.job.domain.entity.JobOutboxStatus;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.persistence.JobOutboxEventRepository;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobOutboxPublisherScheduler {

    private static final int MAX_RETRIES = 10;

    private final JobOutboxEventRepository repository;
    private final JobOutboxService outboxService;
    private final JobPublisher jobPublisher;
    private final JobService jobService;

    @Scheduled(fixedDelay = 500)
    public void publishPending() {
        List<JobOutboxEvent> events = repository.findTop100ByStatusAndNextAttemptAtBeforeOrderByIdAsc(
                JobOutboxStatus.PENDING,
                LocalDateTime.now());

        for (JobOutboxEvent event : events) {
            try {
                JobMessage message = outboxService.deserialize(event.getPayload());
                jobPublisher.send(message);
                markSent(event.getId(), event.getJobId());
            } catch (RuntimeException e) {
                markFailure(event.getId(), event.getJobId(), e);
            }
        }
    }

    @Transactional
    protected void markSent(Long eventId, Long jobId) {
        repository.findById(eventId).ifPresent(event -> event.markSent(LocalDateTime.now()));
        jobService.markPublished(jobId);
    }

    @Transactional
    protected void markFailure(Long eventId, Long jobId, RuntimeException error) {
        repository.findById(eventId)
                .ifPresent(event -> {
                    if (event.getRetryCount() >= MAX_RETRIES) {
                        event.markFailed(LocalDateTime.now(), error);
                        jobService.markSendFailed(jobId);
                        log.error("Outbox send permanently failed. jobId={}", jobId, error);
                    } else {
                        event.markRetry(LocalDateTime.now(), error);
                        log.warn(
                                "Outbox send failed. will retry. jobId={}, retry={}",
                                jobId,
                                event.getRetryCount(),
                                error);
                    }
                });
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupSent() {
        repository.deleteByStatusAndCreatedAtBefore(JobOutboxStatus.SENT, LocalDateTime.now().minusDays(3));
    }
}
