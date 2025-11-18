package com.example.echoshotx.member.presentation.controller;

import com.example.echoshotx.member.application.usecase.TestGenerateTokenUseCase;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Member", description = "회원 관리 API")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final TestGenerateTokenUseCase testGenerateTokenUseCase;

    @Operation
            (summary = "테스트용 토큰 생성",
            description = "테스트용으로 회원 토큰을 생성합니다. 실제 서비스에서는 사용하지 않습니다.")
    @GetMapping("/test-token/{username}")
    public ApiResponseDto<?> generateTestToken(@PathVariable("username") String username) {
        return ApiResponseDto.onSuccess(testGenerateTokenUseCase.execute(username));
    }

}
