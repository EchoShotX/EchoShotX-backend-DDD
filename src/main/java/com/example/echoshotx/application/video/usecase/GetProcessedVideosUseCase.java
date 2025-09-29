package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.infrastructure.service.AwsS3Service;
import com.example.echoshotx.presentation.video.dto.response.VideoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetProcessedVideosUseCase {

    private final VideoAdaptor videoAdaptor;
    private final AwsS3Service awsS3Service;

    public List<VideoListResponse> execute(Member member) {
        // AI 처리 완료된 영상만 조회
        List<Video> processedVideos = videoAdaptor.queryAllByMemberIdAndStatus(
                member.getId(),
                VideoStatus.PROCESSED
        );

        // 각 영상에 대해 썸네일 URL 생성 (스트리밍 기능은 보류)
        return processedVideos.stream()
                .map(this::enrichWithThumbnailUrl)
                .toList();
    }

    /**
     * 영상 정보에 썸네일 URL을 추가합니다
     * 스트리밍 관련 기능은 향후 구현 예정으로 보류
     */
    private VideoListResponse enrichWithThumbnailUrl(Video video) {
        // 기본 Response 생성
        VideoListResponse response = VideoListResponse.from(video);

        // 썸네일 URL 생성 (Public URL 방식 사용)
        if (video.getS3ThumbnailKey() != null) {
            String thumbnailUrl = awsS3Service.generateFileUrl(video.getS3ThumbnailKey());
            response.setThumbnailUrl(thumbnailUrl);
        }

        // 스트리밍 관련 기능은 보류
        // TODO: 향후 스트리밍 기능 구현 시 추가
        // - generateStreamingUrl()
        // - generateDownloadUrl()
        // - urlExpiresAt 설정

        return response;

    }
}
