package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.presentation.dto.request.AuthRequest;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.security.dto.JwtToken;
import com.example.echoshotx.shared.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ReIssueRefreshTokenUseCase {

    private final TokenService tokenService;

    public JwtToken execute(AuthRequest.ReIssue  request) {
        return tokenService.issueTokens(request.getRefreshToken());
    }

}
