package com.example.echoshotx.member.presentation.controller;

import com.example.echoshotx.member.application.usecase.ExchangeCodeUseCase;
import com.example.echoshotx.member.presentation.dto.request.AuthExchangeRequest;
import com.example.echoshotx.member.presentation.dto.response.AuthExchangeResponse;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
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

    @Operation(
            summary = "인증 코드 교환",
            description = "OAuth2 인증 성공 후 받은 1회용 코드를 JWT 토큰으로 교환합니다."
    )
    @PostMapping("/exchange")
    public ApiResponseDto<AuthExchangeResponse> exchangeCode(@Valid @RequestBody AuthExchangeRequest request) {
        AuthExchangeResponse response = exchangeCodeUseCase.execute(request.getCode());
        
        return ApiResponseDto.onSuccess(response);
    }
}

