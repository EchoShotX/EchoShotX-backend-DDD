package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.application.adaptor.JobAdaptor;
import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.video.domain.entity.ProcessingType;
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

    public Job createJob(Member member, Long videoId, String s3Key, ProcessingType processingType) {
        return jobAdaptor.saveJob(Job.create(member.getId(), videoId, s3Key, processingType));
    }

    public void markSendFailed(Long jobId) {
        Job job = jobAdaptor.queryById(jobId);
        job.markFailed();
    }

}
