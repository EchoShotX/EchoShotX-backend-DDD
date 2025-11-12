package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.application.adaptor.JobAdaptor;
import com.example.echoshotx.job.domain.entity.Job;
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
    private final JobPublisher jobPublisher;

    public Long createAndPublishJob(Member member, String s3Key, String taskType) {
        Job job = Job.create(s3Key, taskType);
        jobAdaptor.saveJob(job);

    }
}
