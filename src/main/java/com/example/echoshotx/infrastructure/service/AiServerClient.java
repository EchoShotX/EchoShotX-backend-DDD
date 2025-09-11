package com.example.echoshotx.infrastructure.service;

import com.example.echoshotx.infrastructure.ai.dto.request.*;
import com.example.echoshotx.infrastructure.ai.dto.response.*;
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
public class AiServerClient {
    
    private final WebClient aiServerWebClient;
    
    /**
     * 영상 처리 요청 (A영상 → C영상 변환)
     */
    public Mono<VideoProcessingResponse> processVideo(VideoProcessingRequest request) {
        log.info("AI 서버 영상처리 요청 시작: videoId={}, type={}", 
                request.getVideoId(), request.getProcessingType());
        
        Supplier<Mono<VideoProcessingResponse>> supplier = () ->
            aiServerWebClient.post()
                .uri("/api/video/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VideoProcessingResponse.class)
                .timeout(Duration.ofMinutes(30))
                .doOnSuccess(response -> 
                    log.info("영상처리 요청 성공: videoId={}, status={}", 
                            response.getVideoId(), response.getStatus()))
                .doOnError(error -> 
                    log.error("영상처리 요청 실패: videoId={}, error={}", 
                            request.getVideoId(), error.getMessage()));
        
        return executeWithResilience(supplier, "processVideo");
    }
    
    /**
     * 얼굴 인식 요청
     */
    public Mono<FaceRecognitionResponse> recognizeFaces(FaceRecognitionRequest request) {
        log.info("AI 서버 얼굴인식 요청 시작: videoId={}", request.getVideoId());
        
        Supplier<Mono<FaceRecognitionResponse>> supplier = () ->
            aiServerWebClient.post()
                .uri("/api/face/recognize")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FaceRecognitionResponse.class)
                .timeout(Duration.ofMinutes(10))
                .doOnSuccess(response -> 
                    log.info("얼굴인식 요청 성공: videoId={}, status={}", 
                            response.getVideoId(), response.getStatus()))
                .doOnError(error -> 
                    log.error("얼굴인식 요청 실패: videoId={}, error={}", 
                            request.getVideoId(), error.getMessage()));
        
        return executeWithResilience(supplier, "recognizeFaces");
    }
    
    /**
     * 음악 분석 요청
     */
    public Mono<MusicAnalysisResponse> analyzeMusic(MusicAnalysisRequest request) {
        log.info("AI 서버 음악분석 요청 시작: videoId={}", request.getVideoId());
        
        Supplier<Mono<MusicAnalysisResponse>> supplier = () ->
            aiServerWebClient.post()
                .uri("/api/music/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MusicAnalysisResponse.class)
                .timeout(Duration.ofMinutes(5))
                .doOnSuccess(response -> 
                    log.info("음악분석 요청 성공: videoId={}, status={}", 
                            response.getVideoId(), response.getStatus()))
                .doOnError(error -> 
                    log.error("음악분석 요청 실패: videoId={}, error={}", 
                            request.getVideoId(), error.getMessage()));
        
        return executeWithResilience(supplier, "analyzeMusic");
    }
    
    /**
     * 영상 처리 상태 조회
     */
    public Mono<VideoProcessingResponse> getVideoProcessingStatus(Long videoId) {
        log.debug("영상처리 상태 조회: videoId={}", videoId);
        
        Supplier<Mono<VideoProcessingResponse>> supplier = () ->
            aiServerWebClient.get()
                .uri("/api/video/status/{videoId}", videoId)
                .retrieve()
                .bodyToMono(VideoProcessingResponse.class)
                .timeout(Duration.ofSeconds(30));
        
        return executeWithResilience(supplier, "getVideoProcessingStatus");
    }
    
    /**
     * 얼굴 인식 상태 조회
     */
    public Mono<FaceRecognitionResponse> getFaceRecognitionStatus(Long videoId) {
        log.debug("얼굴인식 상태 조회: videoId={}", videoId);
        
        Supplier<Mono<FaceRecognitionResponse>> supplier = () ->
            aiServerWebClient.get()
                .uri("/api/face/status/{videoId}", videoId)
                .retrieve()
                .bodyToMono(FaceRecognitionResponse.class)
                .timeout(Duration.ofSeconds(30));
        
        return executeWithResilience(supplier, "getFaceRecognitionStatus");
    }
    
    /**
     * 음악 분석 상태 조회
     */
    public Mono<MusicAnalysisResponse> getMusicAnalysisStatus(Long videoId) {
        log.debug("음악분석 상태 조회: videoId={}", videoId);
        
        Supplier<Mono<MusicAnalysisResponse>> supplier = () ->
            aiServerWebClient.get()
                .uri("/api/music/status/{videoId}", videoId)
                .retrieve()
                .bodyToMono(MusicAnalysisResponse.class)
                .timeout(Duration.ofSeconds(30));
        
        return executeWithResilience(supplier, "getMusicAnalysisStatus");
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
