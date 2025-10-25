package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.video.presentation.dto.response.VideoDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetVideoUseCase {

    private final VideoAdaptor videoAdaptor;

    public VideoDetailResponse execute(Long videoId, Member member) {
        Video video = videoAdaptor.queryById(videoId);
        video.validateMember(member);

        VideoDetailResponse response = VideoDetailResponse.from(video);
        
        // TODO: URL 정보 추가 로직 구현 필요
        // 현재는 기본 정보만 반환
        
        return response;
    }
}
