package com.example.echoshotx.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseConnectionManager sseConnectionManager;

    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        int totalConnections = sseConnectionManager.getTotalConnectionCount();
        if (totalConnections == 0) {
            return;
        }
        log.debug("Sending heartbeat to {} SSE connections", totalConnections);

        int successCount = sseConnectionManager.sendHeartbeatToAll();
        log.debug("Heartbeat sent successfully to {} out of {} connections", successCount, totalConnections);

    }

}
