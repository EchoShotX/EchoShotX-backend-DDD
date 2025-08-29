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

    /**
     * 회원의 AI 처리 완료된 영상 목록 조회
     * @param member 로그인한 회원
     * @return AI 처리 완료된 영상 목록
     */
    public List<VideoListResponse> execute(Member member) {
        log.info("회원의 처리 완료된 영상 목록 조회 시작 - 회원ID: {}", member.getId());
        
        // AI 처리 완료된 영상만 조회
        List<Video> processedVideos = videoAdaptor.queryAllByMemberIdAndStatus(
                member.getId(), 
                VideoStatus.PROCESSED
        );
        
        List<VideoListResponse> response = processedVideos.stream()
                .map(VideoListResponse::from)
                .toList();
        
        log.info("회원의 처리 완료된 영상 조회 완료 - 회원ID: {}, 영상 개수: {}", 
                member.getId(), response.size());
        
        return response;
    }
}
