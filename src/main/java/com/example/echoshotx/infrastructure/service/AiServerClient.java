package com.example.echoshotx.infrastructure.service;

import com.example.echoshotx.infrastructure.ai.dto.request.VideoUpScalingRequest;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
// 삭제 예정!!
public class AiServerClient {
    
    private final WebClient aiServerWebClient;

    private static final String UPSCALING_API_URL = "/api/video/process";

    /**
     * 영상 업스케일링 요청 (A영상 → C영상 변환)
     */
    public Mono<VideoProcessingResponse> processVideo(VideoUpScalingRequest request) {
        log.info("AI 서버 영상 업스케일링 요청 시작: videoId={}", request.getVideoId());
        
        Supplier<Mono<VideoProcessingResponse>> supplier = () ->
            aiServerWebClient.post()
                .uri(UPSCALING_API_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VideoProcessingResponse.class)
                .timeout(Duration.ofMinutes(30))
                .doOnSuccess(response -> 
                    log.info("영상 업스케일링 요청 성공: videoId={}, status={}", 
                            response.getVideoId(), response.getStatus()))
                .doOnError(error -> 
                    log.error("영상 업스케일링 요청 실패: videoId={}, error={}", 
                            request.getVideoId(), error.getMessage()));
        
        return executeWithResilience(supplier, "processVideo");
    }
    
    /**
     * 영상 처리 상태 조회
     */
    public Mono<VideoProcessingResponse> getVideoProcessingStatus(Long videoId) {
        Supplier<Mono<VideoProcessingResponse>> supplier = () ->
            aiServerWebClient.get()
                .uri("/api/video/status/{videoId}", videoId)
                .retrieve()
                .bodyToMono(VideoProcessingResponse.class)
                .timeout(Duration.ofSeconds(30));
        
        return executeWithResilience(supplier, "getVideoProcessingStatus");
    }
    
    /**
     * AI 서버 헬스체크
     */
    public Mono<Boolean> healthCheck() {
        log.debug("AI 서버 헬스체크 시작");
        
        Supplier<Mono<Boolean>> supplier = () ->
            aiServerWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .map("OK"::equals)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false);
        
        return executeWithResilience(supplier, "healthCheck");
    }
    
    /**
     * 기본 실행 메서드 (재시도 로직 포함)
     */
    private <T> Mono<T> executeWithResilience(Supplier<Mono<T>> supplier, String operation) {
        return Mono.defer(() -> {
            log.debug("AI 서버 {} 작업 시작", operation);
            return supplier.get()
                .retry(3) // 3번 재시도
                .timeout(Duration.ofMinutes(30)) // 30분 타임아웃
                .doOnSuccess(result -> 
                    log.debug("AI 서버 {} 작업 성공", operation))
                .doOnError(error -> 
                    log.error("AI 서버 {} 작업 실패: {}", operation, error.getMessage()))
                .onErrorMap(throwable -> handleAiServerException(throwable));
        });
    }
    
    /**
     * AI 서버 예외 처리
     */
    private RuntimeException handleAiServerException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            return new RuntimeException(
                String.format("AI 서버 응답 오류: %d - %s", 
                    ex.getStatusCode().value(), ex.getResponseBodyAsString()));
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return new RuntimeException("AI 서버 응답 시간 초과");
        }
        
        if (throwable instanceof java.net.ConnectException) {
            return new RuntimeException("AI 서버 연결 실패");
        }
        
        return new RuntimeException("AI 서버 통신 오류: " + throwable.getMessage(), throwable);
    }
}
