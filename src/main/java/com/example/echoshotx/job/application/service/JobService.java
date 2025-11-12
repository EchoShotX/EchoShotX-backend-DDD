package com.example.echoshotx.job.application.service;

import com.example.echoshotx.job.application.adaptor.JobAdaptor;
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
    private final JobPublisher jobPublisher;

    public void createAndPublishJob(Member member, String s3Key, String taskType) {
        Job job = jobAdaptor.saveJob(Job.create(s3Key, taskType));
        JobMessage jobMessage = JobMessage.builder()
                .jobId(job.getId())
                .taskType(taskType)
                .memberId(member.getId())
                .s3Key(s3Key)
                .build();

        jobPublisher.send(jobMessage);
    }

}
