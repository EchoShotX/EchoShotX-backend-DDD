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
        Video video = videoAdaptor.queryById(videoId);
        video.validateMember(member.getId());
        return VideoDetailResponse.from(video);
    }

}
