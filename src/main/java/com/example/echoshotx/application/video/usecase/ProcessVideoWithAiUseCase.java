package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.service.VideoService;
import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.infrastructure.service.AiServerClient;
import com.example.echoshotx.infrastructure.ai.dto.request.VideoProcessingRequest;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ProcessVideoWithAiUseCase {
    
    private final AiServerClient aiServerClient;
    private final VideoService videoService;
    
    /**
     * AI 서버를 통한 영상처리 실행
     */
    public Mono<VideoProcessingResponse> execute(ProcessVideoCommand command) {
        log.info("AI 영상처리 시작: videoId={}, type={}", 
                command.getVideoId(), command.getProcessingType());
        
        return Mono.fromCallable(() -> {
            // 1. Video 엔티티 조회
            Video video = videoService.findById(command.getVideoId());
            
            // 2. S3에서 A영상 다운로드 URL 생성
            String inputVideoUrl = "https://example.com/raw/" + command.getVideoId() + "/A_video.mp4";
            
            // 3. AI 서버에 영상처리 요청
            VideoProcessingRequest request = VideoProcessingRequest.builder()
                .videoId(command.getVideoId())
                .inputVideoUrl(inputVideoUrl)
                .outputPath("result/" + command.getVideoId() + "/C_video.mp4")
                .processingType(command.getProcessingType())
                .metadata(createVideoMetadata(video))
                .requestedAt(LocalDateTime.now())
                .build();
            
            return aiServerClient.processVideo(request);
        })
        .flatMap(mono -> mono)
        .doOnSuccess(response -> {
            log.info("AI 영상처리 요청 성공: videoId={}, status={}", 
                    response.getVideoId(), response.getStatus());
        })
        .doOnError(error -> {
            log.error("AI 영상처리 요청 실패: videoId={}, error={}", 
                    command.getVideoId(), error.getMessage());
        });
    }
    
    /**
     * Video 엔티티에서 메타데이터 추출
     */
    private VideoProcessingRequest.VideoMetadata createVideoMetadata(Video video) {
        return VideoProcessingRequest.VideoMetadata.builder()
            .fileName(video.getOriginalFileName())
            .fileSize(video.getFileSizeBytes())
            .mimeType("video/mp4")
            .duration(video.getMetadata() != null ? video.getMetadata().getDurationSeconds().intValue() : null)
            .width(video.getMetadata() != null ? video.getMetadata().getWidth() : null)
            .height(video.getMetadata() != null ? video.getMetadata().getHeight() : null)
            .frameRate(video.getMetadata() != null ? video.getMetadata().getFrameRate() : null)
            .build();
    }
    
    /**
     * 영상처리 명령 DTO
     */
    public static class ProcessVideoCommand {
        private final Long videoId;
        private final ProcessingType processingType;
        
        public ProcessVideoCommand(Long videoId, ProcessingType processingType) {
            this.videoId = videoId;
            this.processingType = processingType;
        }
        
        public Long getVideoId() {
            return videoId;
        }
        
        public ProcessingType getProcessingType() {
            return processingType;
        }
    }
}
