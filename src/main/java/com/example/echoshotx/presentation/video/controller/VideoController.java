package com.example.echoshotx.presentation.video.controller;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.service.VideoService;
import com.example.echoshotx.presentation.video.dto.response.VideoUploadResponse;
import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import com.example.echoshotx.infrastructure.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.infrastructure.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Video", description = "영상 업로드 및 관리 API")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(summary = "영상 업로드", description = "영상 파일을 업로드하고 DB에 저장합니다")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseDto<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "processingType", defaultValue = "BASIC_ENHANCEMENT") ProcessingType processingType,
            @CurrentMember Long memberId) {
        
        log.info("영상 업로드 요청 - 사용자ID: {}, 파일명: {}, 처리타입: {}", 
                memberId, file.getOriginalFilename(), processingType);
        
        // 파일 유효성 검증
        validateUploadFile(file);
        
        // 영상 업로드 처리
        Video uploadedVideo = videoService.uploadVideo(file, memberId, processingType);
        
        // 응답 DTO 생성
        VideoUploadResponse response = VideoUploadResponse.from(uploadedVideo);
        
        log.info("영상 업로드 완료 - VideoID: {}", uploadedVideo.getId());
        
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "영상 조회", description = "영상 ID로 영상 정보를 조회합니다")
    @GetMapping("/{videoId}")
    public ApiResponseDto<VideoDetailResponse> getVideo(
            @PathVariable Long videoId,
            @CurrentMember Long memberId) {
        
        log.info("영상 조회 요청 - VideoID: {}, 사용자ID: {}", videoId, memberId);
        
        Video video = videoService.findById(videoId);
        
        // 본인 영상인지 확인 (기본 권한 체크)
        if (!video.getMemberId().equals(memberId)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }
        
        VideoDetailResponse response = VideoDetailResponse.from(video);
        
        return ApiResponseDto.onSuccess(response);
    }

    /**
     * 업로드 파일 유효성 검증
     */
    private void validateUploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("업로드할 파일이 없습니다.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("파일명이 올바르지 않습니다.");
        }
        
        log.info("파일 검증 완료 - 파일명: {}, 크기: {}bytes", originalFilename, file.getSize());
    }
}
