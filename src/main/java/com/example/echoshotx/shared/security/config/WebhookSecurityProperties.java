package com.example.echoshotx.shared.security.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class WebhookSecurityProperties {

    @Value("${app.webhook.secret:}")
    private String secret;

    @Value("${app.webhook.allowed-skew-seconds:300}")
    private long allowedSkewSeconds;
}
