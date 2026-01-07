package com.example.echoshotx.notification.application.service;

import com.example.echoshotx.notification.domain.exception.NotificationErrorStatus;
import com.example.echoshotx.notification.presentation.exception.NotificationHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE (Server-Sent Events) 연결 관리자.
 *
 * <p>
 * 회원별 SSE 연결을 관리하고, 실시간 알림을 전송합니다.
 */
@Slf4j
@Component
public class SseConnectionManager {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 60분
    private static final String SSE_EVENT_NAME = "notification";

    // Key: memberId, Value: List of SseEmitters (다중 디바이스 지원)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(Long memberId) {
        // 기존 연결이 있으면 종료 (새 연결 우선)
        SseEmitter existingEmitter = emitters.get(memberId);
        if (existingEmitter != null) {
            existingEmitter.complete();
            log.info("Existing SSE connection closed for member: {}", memberId);
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(memberId, emitter);

        log.info("SSE connection created for member: {}", memberId);

        // 연결 완료 시 제거
        emitter.onCompletion(
                () -> {
                    removeEmitter(memberId);
                    log.info("SSE connection completed for member: {}", memberId);
                });

        // 타임아웃 시 제거
        emitter.onTimeout(
                () -> {
                    removeEmitter(memberId);
                    log.warn("SSE connection timeout for member: {}", memberId);
                });

        // 에러 시 제거
        emitter.onError(
                e -> {
                    removeEmitter(memberId);
                    log.error("SSE connection error for member: {}, error: {}", memberId, e.getMessage());
                });

        // 연결 확인용 초기 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE connection established"));
        } catch (IOException e) {
            log.error("Failed to send initial connection event to member: {}", memberId, e);
            removeEmitter(memberId);
            throw new NotificationHandler(NotificationErrorStatus.SSE_CONNECTION_FAILED);
        }

        return emitter;
    }

    /**
     * 특정 회원에게 알림 전송.
     *
     * @param memberId 회원 ID
     * @param data     전송할 데이터
     * @return 전송 성공 여부
     */
    public boolean sendToMember(Long memberId, Object data) {
        SseEmitter emitter = emitters.get(memberId);

        if (emitter == null) {
            log.warn("No active SSE connection for member: {}", memberId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(SSE_EVENT_NAME).data(data));
            log.info("Notification sent to member: {}", memberId);
            return true;
        } catch (IOException e) {
            log.error("Failed to send notification to member: {}, removing dead emitter", memberId, e);
            removeEmitter(memberId);
            return false;
        }
    }

    /**
     * 여러 회원에게 브로드캐스트.
     *
     * @param memberIds 회원 ID 목록
     * @param data      전송할 데이터
     */
    public void broadcast(List<Long> memberIds, Object data) {
        log.info("Broadcasting notification to {} members", memberIds.size());
        for (Long memberId : memberIds) {
            sendToMember(memberId, data);
        }
    }

    /**
     * 모든 연결된 회원에게 브로드캐스트.
     *
     * @param data 전송할 데이터
     */
    public void broadcastToAll(Object data) {
        log.info("Broadcasting notification to all {} members", emitters.size());
        emitters.keySet().forEach(memberId -> sendToMember(memberId, data));
    }

    /**
     * 모든 연결에 Heartbeat 전송하여 dead connection 조기 감지
     *
     * @return 성공한 전송 수
     */
    public int sendHeartbeatToAll() {
        int successCount = 0;
        int failCount = 0;
        List<Long> deadMemberIds = new ArrayList<>();

        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().comment("heartbeat"));
                successCount++;
            } catch (IOException | IllegalStateException e) {
                deadMemberIds.add(entry.getKey());
                failCount++;
            }
        }

        // dead emitter 즉시 제거
        deadMemberIds.forEach(this::removeEmitter);

        if (failCount > 0) {
            log.info("Heartbeat completed: success={}, removed={} dead connections",
                    successCount, failCount);
        }

        return successCount;
    }

    /**
     * 특정 회원의 연결 해제.
     *
     * @param memberId 회원 ID
     */
    public void disconnectMember(Long memberId) {
        SseEmitter emitter = emitters.remove(memberId);
        if (emitter != null) {
            emitter.complete();
            log.info("Disconnected SSE connection for member: {}", memberId);
        }
    }

    /**
     * 특정 회원의 활성 연결 수 조회
     */
    public int getConnectionCount(Long memberId) {
        return emitters.containsKey(memberId) ? 1 : 0;
    }

    /**
     * 전체 활성 연결 수 조회.
     *
     * @return 전체 연결 수
     */
    public int getTotalConnectionCount() {
        return emitters.size();
    }

    /**
     * 특정 회원이 연결되어 있는지 확인.
     *
     * @param memberId 회원 ID
     * @return 연결 여부
     */
    public boolean isConnected(Long memberId) {
        return emitters.containsKey(memberId);
    }

    /**
     * Emitter 제거 (내부 메서드).
     */
    private void removeEmitter(Long memberId) {
        emitters.remove(memberId);
    }

    /**
     * 모든 연결 해제 (서버 종료 시 사용).
     */
    public void disconnectAll() {
        log.info("Disconnecting all SSE connections, total: {}", emitters.size());
        emitters.values().forEach(SseEmitter::complete);
        emitters.clear();
    }
}
