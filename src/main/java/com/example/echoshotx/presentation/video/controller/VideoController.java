package com.example.echoshotx.presentation.video.controller;

import com.example.echoshotx.application.video.usecase.GetVideoUseCase;
import com.example.echoshotx.application.video.usecase.GetProcessedVideosUseCase;
import com.example.echoshotx.application.video.usecase.UpScalingVideoUseCase;
import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.presentation.video.dto.response.VideoUploadResponse;
import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import com.example.echoshotx.presentation.video.dto.response.VideoListResponse;
import com.example.echoshotx.infrastructure.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.infrastructure.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Tag(name = "Video", description = "영상 업로드 및 관리 API")
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final UpScalingVideoUseCase upScalingVideoUseCase;
    private final GetVideoUseCase getVideoUseCase;
    private final GetProcessedVideosUseCase getProcessedVideosUseCase;

    @Operation(summary = "영상 업로드", description = "영상 파일을 업로드하고 DB에 저장합니다")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseDto<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "processingType", defaultValue = "BASIC_ENHANCEMENT") ProcessingType processingType,
            @CurrentMember Member member) {

        VideoUploadResponse response = upScalingVideoUseCase.execute(file, member);

        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "영상 조회", description = "영상 ID로 영상 정보를 조회합니다")
    @GetMapping("/{videoId}")
    public ApiResponseDto<VideoDetailResponse> getVideo(
            @PathVariable Long videoId,
            @CurrentMember Member member) {
        
        VideoDetailResponse response = getVideoUseCase.execute(videoId, member);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "AI 처리 완료된 영상 목록 조회", 
               description = "로그인한 사용자의 AI 처리가 완료된 영상 목록을 조회합니다")
    @GetMapping("/processed")
    public ApiResponseDto<List<VideoListResponse>> getProcessedVideos(
            @CurrentMember Member member) {
        
        List<VideoListResponse> response = getProcessedVideosUseCase.execute(member);
        return ApiResponseDto.onSuccess(response);
    }

}
