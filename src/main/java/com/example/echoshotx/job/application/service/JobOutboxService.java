package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.domain.entity.JobOutboxEvent;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.persistence.JobOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobOutboxService {

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
