package com.example.echoshotx.job.application.handler;

import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventHandler {

    private final JobPublisher jobPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreate(JobCreatedEvent event) {
        JobMessage message = JobMessage.builder()
                .jobId(event.getJobId())
                .videoId(event.getVideoId())
                .taskType(event.getTaskType())
                .memberId(event.getMemberId())
                .s3Key(event.getS3Key())
                .build();

        jobPublisher.sendWithRetry(message);
    }

}
