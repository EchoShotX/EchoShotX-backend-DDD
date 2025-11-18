package com.example.echoshotx.job.application.adaptor;

import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.domain.exception.JobErrorStatus;
import com.example.echoshotx.job.infrastructure.persistence.JobRepository;
import com.example.echoshotx.job.presentation.exception.JobHandler;
import com.example.echoshotx.shared.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobAdaptor {

    private final JobRepository jobRepository;

    public Job queryById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobHandler(JobErrorStatus.JOB_NOT_FOUND));
    }

    @Transactional
    public Job saveJob(Job job) {
        return jobRepository.save(job);
    }

}
