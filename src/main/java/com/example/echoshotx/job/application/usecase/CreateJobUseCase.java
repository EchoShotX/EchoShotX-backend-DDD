package com.example.echoshotx.job.application.usecase;

import com.example.echoshotx.job.application.service.JobService;
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

    public void execute(Member member, JobRequest.Create request) {
        jobService.createAndPublishJob(member, request.getS3Key(), request.getTaskType());
    }
}
