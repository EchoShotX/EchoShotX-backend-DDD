package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.service.VideoService;
import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.presentation.video.dto.response.VideoUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class UploadVideoUseCase {

    private final VideoService videoService;

    /**
     * 영상 업로드 및 AI 업스케일링 요청 처리
     * 현재는 화질 업스케일 요청만 지원
     */
    public VideoUploadResponse execute(MultipartFile file, Member member) {
        log.info("영상 업로드 요청 - 사용자: {}, 파일명: {}", member.getId(), file.getOriginalFilename());
        
        // AI 업스케일링으로 고정 처리
        Video uploadedVideo = videoService.uploadVideo(file, member.getId(), ProcessingType.AI_UPSCALING);
        
        log.info("영상 업로드 완료 - Video ID: {}", uploadedVideo.getId());
        return VideoUploadResponse.from(uploadedVideo);
    }

}

