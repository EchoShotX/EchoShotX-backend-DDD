package com.example.echoshotx.job.presentation.controller;

import com.example.echoshotx.job.application.usecase.CreateJobUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "Job", description = "SQS Job 관리 API")
@RequiredArgsConstructor
@RequestMapping("/jobs")
public class JobController {

    private final CreateJobUseCase createJobUseCase;



}
