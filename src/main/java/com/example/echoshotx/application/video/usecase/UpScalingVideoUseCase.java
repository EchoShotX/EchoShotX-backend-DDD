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
public class UpScalingVideoUseCase {

    private final VideoService videoService;

    /**
     * 영상 업로드 및 AI 업스케일링 요청 처리
     */
    public VideoUploadResponse execute(MultipartFile file, Member member) {
        Video uploadedVideo = videoService.uploadVideo(file, member.getId(), ProcessingType.AI_UPSCALING);
        return VideoUploadResponse.from(uploadedVideo);
    }

}

