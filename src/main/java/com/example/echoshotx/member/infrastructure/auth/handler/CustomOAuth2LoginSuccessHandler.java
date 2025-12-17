package com.example.echoshotx.member.infrastructure.auth.handler;

import com.example.echoshotx.shared.redis.service.RedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Component
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RedisService redisService;
    private final String deepLinkScheme;
    private final int codeTtlSeconds;

    public CustomOAuth2LoginSuccessHandler(
            RedisService redisService,
            @Value("${auth.deep-link-scheme:echoshotx}") String deepLinkScheme,
            @Value("${auth.code-ttl-seconds:300}") int codeTtlSeconds) {
        this.redisService = redisService;
        this.deepLinkScheme = deepLinkScheme;
        this.codeTtlSeconds = codeTtlSeconds;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        log.info("--------------------------- OAuth2LoginSuccessHandler ---------------------------");

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Authentication is not OAuth2AuthenticationToken");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid authentication type");
            return;
        }

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oauth2Token.getAuthorizedClientRegistrationId();
        Collection<GrantedAuthority> authorities = oauth2Token.getAuthorities();
        authorities.forEach(grantedAuthority -> log.info("role {}", grantedAuthority.getAuthority()));

        // 애플리케이션 전용이므로 모든 요청을 모바일로 처리
        handleMobileAuth(request, response, authentication);
    }

    private void handleMobileAuth(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 1회용 인증 코드 생성
        String code = UUID.randomUUID().toString();
        String username = authentication.getName();
        
        log.info("Generating one-time auth code for mobile client. Username: {}", username);

        // Redis에 코드 저장 (TTL: 설정된 시간)
        redisService.setAuthCode(code, username, Duration.ofSeconds(codeTtlSeconds));

        // 딥링크로 리다이렉트
        String deepLinkUrl = deepLinkScheme + "://auth/callback?code=" + code;
        log.info("Redirecting to deep link: {}", deepLinkUrl);
        
        response.sendRedirect(deepLinkUrl);
    }
}