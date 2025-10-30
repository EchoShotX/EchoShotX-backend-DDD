package com.example.echoshotx.video.presentation.controller;

import com.example.echoshotx.video.application.usecase.GetVideoUseCase;
import com.example.echoshotx.video.application.usecase.InitiateVideoUploadUseCase;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.video.presentation.dto.request.InitiateUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.InitiateUploadResponse;
import com.example.echoshotx.video.presentation.dto.response.VideoDetailResponse;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.shared.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Video", description = "영상 업로드 및 관리 API")
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    // UseCases
    private final InitiateVideoUploadUseCase initiateVideoUploadUseCase;
    private final GetVideoUseCase getVideoUseCase;

    @Operation(summary = "영상 조회", description = "영상 ID로 영상 정보를 조회합니다")
    @GetMapping("/{videoId}")
    public ApiResponseDto<VideoDetailResponse> getVideo(
            @PathVariable Long videoId,
            @CurrentMember Member member) {
        
        VideoDetailResponse response = getVideoUseCase.execute(videoId, member);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(
            summary = "영상 업로드 시작",
            description = "영상 업로드를 위한 Presigned URL을 발급받습니다. " +
                    "이 URL로 클라이언트가 직접 S3에 업로드합니다."
    )
    @PostMapping("/upload/initiate")
    public ApiResponseDto<InitiateUploadResponse> initiateUpload(
            @Valid @RequestBody InitiateUploadRequest request,
            @CurrentMember Member member
    ) {
        InitiateUploadResponse response = initiateVideoUploadUseCase.execute(request, member);
        return ApiResponseDto.onSuccess(response);
    }

}
