package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.presentation.video.dto.response.VideoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetMemberProcessedVideosUseCase {

    private final VideoAdaptor videoAdaptor;

    public List<VideoListResponse> execute(Member member) {
        // AI 처리 완료된 영상만 조회
        List<Video> processedVideos = videoAdaptor.queryAllByMemberIdAndStatus(
                member.getId(), 
                VideoStatus.PROCESSED
        );
        
        return processedVideos.stream()
                .map(VideoListResponse::from)
                .toList();
    }
}
