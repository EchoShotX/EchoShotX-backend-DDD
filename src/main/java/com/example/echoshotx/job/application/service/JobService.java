package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.application.adaptor.JobAdaptor;
import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import com.example.echoshotx.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JobService {

    private final JobAdaptor jobAdaptor;
    private final JobEventHandler jobEventHandler;

    public void createAndPublishJob(Member member, Long videoId, String s3Key, String taskType) {
        Job job = jobAdaptor.saveJob(Job.create(member.getId(), videoId, s3Key, taskType));
        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(job.getId())
                .videoId(videoId)
                .taskType(taskType)
                .memberId(member.getId())
                .s3Key(s3Key)
                .build();
        jobEventHandler.handleCreate(event);
    }

    public void markSendFailed(Long jobId) {
        Job job = jobAdaptor.queryById(jobId);
        job.markFailed();
    }

}
