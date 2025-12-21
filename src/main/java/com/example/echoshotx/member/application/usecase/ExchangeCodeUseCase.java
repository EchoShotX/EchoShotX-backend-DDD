package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.application.adaptor.MemberAdaptor;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.presentation.dto.response.AuthExchangeResponse;
import com.example.echoshotx.member.presentation.exception.MemberHandler;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;
import com.example.echoshotx.shared.redis.service.RedisService;
import com.example.echoshotx.shared.security.dto.JwtToken;
import com.example.echoshotx.shared.security.service.TokenService;
import com.example.echoshotx.shared.security.vo.CustomUserDetails;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@UseCase
@Transactional
@RequiredArgsConstructor
public class ExchangeCodeUseCase {

    private final RedisService redisService;
    private final TokenService tokenService;
    private final MemberAdaptor memberAdaptor;

    private static final int DEFAULT_EXPIRES_IN = 1800; // 30분 (초 단위)

    public AuthExchangeResponse execute(String code) {
        String username = redisService.getAuthCode(code);
        
        if (username == null) {
            throw new MemberHandler(ErrorStatus.AUTH_CODE_INVALID);
        }

        Member member = memberAdaptor.queryById(Long.parseLong(username));

        CustomUserDetails userDetails = new CustomUserDetails(member);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority(
                        "ROLE_USER"))
        );

        JwtToken jwtToken = tokenService.generateToken(auth);

        // code 삭제 (1회 사용 보장)
        redisService.deleteAuthCode(code);

        return AuthExchangeResponse.builder()
                .accessToken(jwtToken.getAccessToken())
                .refreshToken(jwtToken.getRefreshToken())
                .expiresIn(DEFAULT_EXPIRES_IN)
                .build();
    }

}

