package com.example.echoshotx.shared.security.infrastructure;

import com.example.echoshotx.shared.security.domain.WebhookNonceRecord;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookNonceRepository extends JpaRepository<WebhookNonceRecord, Long> {

    boolean existsByNonce(String nonce);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
