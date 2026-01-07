package com.example.echoshotx.notification.application.service;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE Heartbeat 통합 테스트.
 *
 * <p>
 * 테스트 목적:
 * <ol>
 * <li>Heartbeat 메커니즘으로 Dead Connection 조기 감지 검증</li>
 * <li>Before/After 메모리 수치화 비교</li>
 * <li>연결 수 변화 측정</li>
 * </ol>
 */
@DisplayName("SSE Heartbeat 통합 테스트")
class SseHeartbeatIntegrationTest {

    private SseConnectionManager sseConnectionManager;

    @BeforeEach
    void setUp() {
        sseConnectionManager = new SseConnectionManager();
    }

    @Nested
    @DisplayName("Heartbeat 전송 기본 테스트")
    class HeartbeatBasicTest {

        @Test
        @DisplayName("성공: 정상 연결에 Heartbeat 전송")
        void sendHeartbeatToAll_Success_WithActiveConnections() {
            // Given
            int connectionCount = 10;
            for (long i = 1; i <= connectionCount; i++) {
                sseConnectionManager.createConnection(i);
            }
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(connectionCount);

            // When
            int successCount = sseConnectionManager.sendHeartbeatToAll();

            // Then
            assertThat(successCount).isEqualTo(connectionCount);
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(connectionCount);
        }

        @Test
        @DisplayName("성공: 연결 없을 때 Heartbeat 전송 시 예외 없음")
        void sendHeartbeatToAll_Success_WithNoConnections() {
            // Given
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(0);

            // When
            int successCount = sseConnectionManager.sendHeartbeatToAll();

            // Then
            assertThat(successCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Dead Connection 조기 감지 테스트 (핵심)")
    class DeadConnectionDetectionTest {

        @Test
        @DisplayName("성공: Heartbeat로 Dead Connection 즉시 감지 및 제거")
        void sendHeartbeatToAll_RemovesDeadConnections_Immediately() throws Exception {
            // Given - 10개 연결 생성
            int totalConnections = 10;
            int deadConnections = 5;

            for (long i = 1; i <= totalConnections; i++) {
                sseConnectionManager.createConnection(i);
            }
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(totalConnections);

            // 5개를 Dead 상태로 만듦 (complete 호출로 전송 불가 상태)
            for (long i = 1; i <= deadConnections; i++) {
                // Reflection으로 내부 emitter 맵에 접근하여 complete 호출
                completeEmitterForMember(i);
            }

            // When - Heartbeat 전송
            int successCount = sseConnectionManager.sendHeartbeatToAll();

            // Then
            int aliveConnections = totalConnections - deadConnections;
            assertThat(successCount).isEqualTo(aliveConnections);
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(aliveConnections);

            // 검증 로그 출력
            System.out.println("=== Dead Connection 감지 테스트 결과 ===");
            System.out.println("총 연결 수: " + totalConnections);
            System.out.println("Dead 연결 수: " + deadConnections);
            System.out.println("Heartbeat 성공 수: " + successCount);
            System.out.println("남은 연결 수: " + sseConnectionManager.getTotalConnectionCount());
        }

        private void completeEmitterForMember(Long memberId) throws Exception {
            Field emittersField = SseConnectionManager.class.getDeclaredField("emitters");
            emittersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) emittersField.get(sseConnectionManager);

            SseEmitter emitter = emitters.get(memberId);
            if (emitter != null) {
                emitter.complete(); // 연결 종료 (IOException 발생 유도)
            }
        }
    }

    @Nested
    @DisplayName("메모리 수치화 비교 테스트")
    class MemoryMeasurementTest {

        @Test
        @DisplayName("Before vs After: Dead Connection 제거 시 연결 수 변화 측정")
        void measureConnectionCountChange_BeforeAndAfterHeartbeat() throws Exception {
            System.out.println("\n========================================");
            System.out.println("📊 Before vs After 연결 수 비교 테스트");
            System.out.println("========================================\n");

            // === Before 시나리오 (Heartbeat 없이) ===
            int totalConnections = 100;
            int deadConnections = 50;

            // 100개 연결 생성
            for (long i = 1; i <= totalConnections; i++) {
                sseConnectionManager.createConnection(i);
            }

            int beforeCount = sseConnectionManager.getTotalConnectionCount();
            System.out.println("[Before] 초기 연결 수: " + beforeCount);

            // 50개를 Dead 상태로 만듦
            for (long i = 1; i <= deadConnections; i++) {
                completeEmitterForMember(i);
            }

            // Before: Heartbeat 없이 연결 수 확인
            int beforeHeartbeatCount = sseConnectionManager.getTotalConnectionCount();
            System.out.println("[Before] Dead 발생 후 연결 수 (Heartbeat 전): " + beforeHeartbeatCount);
            System.out.println("→ Dead emitter " + deadConnections + "개가 여전히 Map에 존재");

            // === After 시나리오 (Heartbeat 적용) ===
            int successCount = sseConnectionManager.sendHeartbeatToAll();
            int afterHeartbeatCount = sseConnectionManager.getTotalConnectionCount();

            System.out.println("\n[After] Heartbeat 전송 후 연결 수: " + afterHeartbeatCount);
            System.out.println("→ Heartbeat 성공: " + successCount + "개");
            System.out.println("→ Dead emitter " + (beforeHeartbeatCount - afterHeartbeatCount) + "개 즉시 제거됨");

            // === 결과 비교 ===
            System.out.println("\n========================================");
            System.out.println("📈 결과 비교");
            System.out.println("========================================");
            System.out.println("| 시점 | 연결 수 | 상태 |");
            System.out.println("|------|--------|------|");
            System.out.println("| Before (초기) | " + beforeCount + " | 100% 유지 |");
            System.out.println("| Before (Dead 발생) | " + beforeHeartbeatCount + " | Dead 포함 |");
            System.out.println("| After (Heartbeat) | " + afterHeartbeatCount + " | Dead 제거 |");
            System.out.println("========================================\n");

            // Assertions
            assertThat(beforeCount).isEqualTo(totalConnections);
            assertThat(beforeHeartbeatCount).isEqualTo(totalConnections); // Dead여도 Map에 존재
            assertThat(afterHeartbeatCount).isEqualTo(totalConnections - deadConnections);
        }

        @Test
        @DisplayName("메모리 사용량 측정: 연결 생성/제거 전후 비교")
        void measureMemoryUsage_BeforeAndAfterHeartbeat() throws Exception {
            System.out.println("\n========================================");
            System.out.println("💾 메모리 사용량 측정 테스트");
            System.out.println("========================================\n");

            Runtime runtime = Runtime.getRuntime();

            // GC 실행하여 초기 상태 정리
            System.gc();
            Thread.sleep(100);

            long initialMemory = getUsedMemory(runtime);
            System.out.println("[초기] 메모리 사용량: " + formatBytes(initialMemory));

            // 100개 연결 생성
            int totalConnections = 100;
            for (long i = 1; i <= totalConnections; i++) {
                sseConnectionManager.createConnection(i);
            }

            long afterCreationMemory = getUsedMemory(runtime);
            long memoryForConnections = afterCreationMemory - initialMemory;
            System.out.println("[연결 생성 후] 메모리 사용량: " + formatBytes(afterCreationMemory));
            System.out.println("→ 100개 연결에 사용된 메모리: " + formatBytes(memoryForConnections));
            System.out.println("→ 연결당 평균 메모리: " + formatBytes(memoryForConnections / totalConnections));

            // 50개를 Dead 상태로 만듦
            int deadConnections = 50;
            for (long i = 1; i <= deadConnections; i++) {
                completeEmitterForMember(i);
            }

            long afterDeadMemory = getUsedMemory(runtime);
            System.out.println("\n[Dead 발생 후] 메모리 사용량: " + formatBytes(afterDeadMemory));
            System.out.println("→ Dead emitter 50개가 여전히 메모리 점유");

            // Heartbeat로 Dead 제거
            sseConnectionManager.sendHeartbeatToAll();
            System.gc();
            Thread.sleep(100);

            long afterHeartbeatMemory = getUsedMemory(runtime);
            long freedMemory = afterDeadMemory - afterHeartbeatMemory;
            System.out.println("\n[Heartbeat 후] 메모리 사용량: " + formatBytes(afterHeartbeatMemory));
            System.out.println("→ 해제된 메모리: " + formatBytes(freedMemory));

            // 결과 요약
            System.out.println("\n========================================");
            System.out.println("📊 메모리 측정 결과 요약");
            System.out.println("========================================");
            System.out.println("| 시점 | 메모리 | 연결 수 |");
            System.out.println("|------|--------|--------|");
            System.out.println("| 초기 | " + formatBytes(initialMemory) + " | 0 |");
            System.out.println("| 100개 생성 | " + formatBytes(afterCreationMemory) + " | 100 |");
            System.out.println("| 50개 Dead | " + formatBytes(afterDeadMemory) + " | 100 (Dead 포함) |");
            System.out.println("| Heartbeat 후 | " + formatBytes(afterHeartbeatMemory) + " | 50 |");
            System.out.println("========================================\n");

            // 연결 수 검증
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(50);
        }

        private long getUsedMemory(Runtime runtime) {
            return runtime.totalMemory() - runtime.freeMemory();
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024)
                return bytes + " B";
            if (bytes < 1024 * 1024)
                return String.format("%.2f KB", bytes / 1024.0);
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }

        private void completeEmitterForMember(Long memberId) throws Exception {
            Field emittersField = SseConnectionManager.class.getDeclaredField("emitters");
            emittersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) emittersField.get(sseConnectionManager);

            SseEmitter emitter = emitters.get(memberId);
            if (emitter != null) {
                emitter.complete();
            }
        }
    }

    @Nested
    @DisplayName("SseHeartbeatScheduler 테스트")
    class HeartbeatSchedulerTest {

        @Test
        @DisplayName("성공: 스케줄러가 Heartbeat 정상 호출")
        void scheduler_CallsHeartbeat_Successfully() {
            // Given
            SseHeartbeatScheduler scheduler = new SseHeartbeatScheduler(sseConnectionManager);

            for (long i = 1; i <= 5; i++) {
                sseConnectionManager.createConnection(i);
            }

            // When - 스케줄러 메서드 직접 호출
            assertThatCode(() -> scheduler.sendHeartbeat())
                    .doesNotThrowAnyException();

            // Then
            assertThat(sseConnectionManager.getTotalConnectionCount()).isEqualTo(5);
        }
    }
}
