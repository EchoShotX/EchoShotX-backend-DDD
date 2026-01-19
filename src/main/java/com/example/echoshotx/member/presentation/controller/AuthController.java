package com.example.echoshotx.member.presentation.controller;

import com.example.echoshotx.member.application.usecase.ExchangeCodeUseCase;
import com.example.echoshotx.member.application.usecase.LogoutUseCase;
import com.example.echoshotx.member.application.usecase.ReIssueRefreshTokenUseCase;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.presentation.dto.request.AuthExchangeRequest;
import com.example.echoshotx.member.presentation.dto.request.AuthRequest;
import com.example.echoshotx.member.presentation.dto.response.AuthExchangeResponse;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.shared.security.aop.CurrentMember;
import com.example.echoshotx.shared.security.dto.JwtToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ExchangeCodeUseCase exchangeCodeUseCase;
    private final ReIssueRefreshTokenUseCase reIssueRefreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @Operation(summary = "인증 코드 교환", description = "OAuth2 인증 성공 후 받은 1회용 코드를 JWT 토큰으로 교환합니다.")
    @PostMapping("/exchange")
    public ApiResponseDto<AuthExchangeResponse> exchangeCode(@Valid @RequestBody AuthExchangeRequest request) {
        AuthExchangeResponse response = exchangeCodeUseCase.execute(request.getCode());

        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "토큰 재발급", description = "토큰 재발급")
    @PostMapping("/reIssue")
    public ApiResponseDto<JwtToken> reIssueRefreshToken(@RequestBody AuthRequest.ReIssue request) {
        return ApiResponseDto.onSuccess(reIssueRefreshTokenUseCase.execute(request));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하고 SSE 연결을 해제합니다.")
    @PostMapping("/logout")
    public ApiResponseDto<String> logout(
            @RequestBody AuthRequest.Logout request,
            @CurrentMember Member member) {

        logoutUseCase.execute(request, member);
        return ApiResponseDto.onSuccess("로그아웃이 완료되었습니다.");
    }

}
