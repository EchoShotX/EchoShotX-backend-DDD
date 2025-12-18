package com.example.echoshotx.job.application.usecase;

import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.presentation.dto.request.JobRequest;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateJobUseCase {

    private final JobService jobService;
    private final JobEventHandler jobEventHandler;

    public void execute(Member member, JobRequest.Create request) {
        Job job = jobService.createJob(member, request.getVideoId(), request.getS3Key(), request.getTaskType());
        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(job.getId())
                .videoId(job.getVideoId())
                .process(job.getProcessingType())
                .memberId(member.getId())
                .s3Key(job.getS3Key())
                .build();
        jobEventHandler.handleCreate(event);
    }
}
