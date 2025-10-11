package com.example.echoshotx.presentation.video.controller;

import com.example.echoshotx.application.video.usecase.GetVideoUseCase;
import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import com.example.echoshotx.infrastructure.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.infrastructure.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Video", description = "영상 업로드 및 관리 API")
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final GetVideoUseCase getVideoUseCase;

    @Operation(summary = "영상 조회", description = "영상 ID로 영상 정보를 조회합니다")
    @GetMapping("/{videoId}")
    public ApiResponseDto<VideoDetailResponse> getVideo(
            @PathVariable Long videoId,
            @CurrentMember Member member) {
        
        VideoDetailResponse response = getVideoUseCase.execute(videoId, member);
        return ApiResponseDto.onSuccess(response);
    }


}
