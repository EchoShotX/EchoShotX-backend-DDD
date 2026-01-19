package com.example.echoshotx.shared.security.filter;

import com.example.echoshotx.shared.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 필터를 건너뛸 경로 목록 (인증 없이 접근 가능한 테스트 API 등)
    private static final List<String> SKIP_FILTER_PATHS = List.of(
            "/test/notifications/**",
            "/actuator/health");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // /api prefix 제거
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }

        String finalPath = path;
        return SKIP_FILTER_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, finalPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // HttpServletRequest에서 JWT 토큰 추출
        HttpServletRequest httpServletRequest = ((HttpServletRequest) request);

        String requestURI = httpServletRequest.getRequestURI();
        String token = resolveToken(request);

        if (token != null && tokenService.validateToken(token)) {
            // 토큰이 유효할 경우 토큰에서 Authentication 객체를 가지고 와서 SecurityContext에 저장
            Authentication authentication = tokenService.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("username", authentication.getName());
            log.info("set Authentication to security context for '{}', uri: '{}', Role '{}'",
                    authentication.getName(), ((HttpServletRequest) request).getRequestURI(),
                    authentication.getAuthorities());
        } else {
            String uri = ((HttpServletRequest) request).getRequestURI();
            if (!uri.equals("/health")) {
                log.info("no valid JWT token found, uri: {}", uri);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}
