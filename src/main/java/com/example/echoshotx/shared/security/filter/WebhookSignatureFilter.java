package com.example.echoshotx.shared.security.filter;

import com.example.echoshotx.shared.security.config.WebhookSecurityProperties;
import com.example.echoshotx.shared.security.service.WebhookReplayGuardService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String NONCE_HEADER = "X-Nonce";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final WebhookSecurityProperties properties;
    private final WebhookReplayGuardService replayGuardService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(PATH_MATCHER.match("/videos/webhook/**", path)
                || PATH_MATCHER.match("/api/videos/webhook/**", path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        if (!StringUtils.hasText(properties.getSecret())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook secret not configured");
            return;
        }

        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (!StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing webhook signature headers");
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid webhook timestamp");
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > properties.getAllowedSkewSeconds()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook timestamp expired");
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String signingPayload =
                timestamp
                        + "."
                        + nonce
                        + "."
                        + wrappedRequest.getMethod()
                        + "."
                        + wrappedRequest.getRequestURI()
                        + "."
                        + wrappedRequest.getCachedBodyAsString();
        String expectedSignature = hmacSha256Hex(signingPayload, properties.getSecret());

        if (!constantTimeEquals(expectedSignature, signature.trim().toLowerCase())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid webhook signature");
            return;
        }

        if (!replayGuardService.registerNonce(nonce)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook replay detected");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(key);
            return HexFormat.of().formatHex(hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Webhook HMAC generation failed", e);
            throw new IllegalStateException("Webhook HMAC generation failed", e);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
