package com.example.demo.infrastructure.auth.handler;

import com.example.demo.infrastructure.security.dto.JwtToken;
import com.example.demo.infrastructure.security.service.TokenService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String REDIRECT_URI;
    private final TokenService tokenService;

    public CustomOAuth2LoginSuccessHandler(
            @Value("${auth.success-redirect-uri}") String redirectUri, TokenService tokenService) {
        this.REDIRECT_URI = redirectUri;
        this.tokenService = tokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        log.info("--------------------------- OAuth2LoginSuccessHandler ---------------------------");
        JwtToken jwtToken = tokenService.generateToken(authentication);
        String provider = null;

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            provider = oauth2Token.getAuthorizedClientRegistrationId();
            Collection<GrantedAuthority> authorities = oauth2Token.getAuthorities();
            authorities.forEach(grantedAuthority -> log.info("role {}", grantedAuthority.getAuthority()));
        }

        String url = UriComponentsBuilder.fromHttpUrl(REDIRECT_URI)
                .queryParam("accessToken", jwtToken.getAccessToken())
                .queryParam("refreshToken", jwtToken.getRefreshToken())
                .queryParam("provider", provider)
                .build()
                .toUriString();
        response.addHeader("Authorization",
                jwtToken.getGrantType() + " " + jwtToken.getAccessToken());

        response.sendRedirect(url);

    }
}