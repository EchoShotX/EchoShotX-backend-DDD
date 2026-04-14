package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.domain.entity.JobOutboxEvent;
import com.example.echoshotx.job.domain.entity.JobOutboxStatus;
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
                outboxService.markSent(event.getId());
                jobService.markPublished(event.getJobId());
            } catch (RuntimeException e) {
                boolean permanentFail = outboxService.markRetryOrFailed(event.getId(), e);
                if (permanentFail) {
                    jobService.markSendFailed(event.getJobId());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupSent() {
        repository.deleteByStatusAndCreatedAtBefore(JobOutboxStatus.SENT, LocalDateTime.now().minusDays(3));
    }
}
