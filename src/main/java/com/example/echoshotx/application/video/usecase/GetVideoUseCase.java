package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetVideoUseCase {

    private final VideoAdaptor videoAdaptor;

    public VideoDetailResponse execute(Long videoId, Member member) {
        log.info("Fetching video details for video: {}, user: {}", videoId, member.getId());
        
        Video video = videoAdaptor.queryById(videoId);
        video.validateMember(member.getId());
        
        // 기본 Response 생성
        VideoDetailResponse response = VideoDetailResponse.from(video);
        
        // TODO: URL 정보 추가 로직 구현 필요
        // 현재는 기본 정보만 반환
        
        return response;
    }
}
