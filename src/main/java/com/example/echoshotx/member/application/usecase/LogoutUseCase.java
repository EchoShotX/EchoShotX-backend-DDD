package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.presentation.dto.request.AuthRequest;
import com.example.echoshotx.notification.application.service.SseConnectionManager;
import com.example.echoshotx.shared.security.aop.CurrentMember;
import com.example.echoshotx.shared.security.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final TokenService tokenService;
    private final SseConnectionManager sseConnectionManager;

    public void execute(AuthRequest.Logout request, Member member) {
        tokenService.logout(request.getRefreshToken());

        // SSE 연결 해제
        if (sseConnectionManager.isConnected(member.getId())) {
            sseConnectionManager.disconnectMember(member.getId());
        }
    }

}
