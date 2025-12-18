package com.example.echoshotx.job.presentation.controller;

import com.example.echoshotx.job.application.usecase.CreateJobUseCase;
import com.example.echoshotx.job.presentation.dto.request.JobRequest;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.exception.payload.code.SuccessStatus;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.shared.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "Job", description = "SQS Job 관리 API")
@RequiredArgsConstructor
@RequestMapping("/jobs")
public class JobController {


}
