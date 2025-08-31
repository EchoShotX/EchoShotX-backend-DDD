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
        log.info("Fetching processed videos for member: {}", member.getId());
        
        // AI 처리 완료된 영상만 조회
        List<Video> processedVideos = videoAdaptor.queryAllByMemberIdAndStatus(
                member.getId(), 
                VideoStatus.PROCESSED
        );
        
        log.info("Found {} processed videos for member: {}", processedVideos.size(), member.getId());
        
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
        try {
            // 기본 Response 생성
            VideoListResponse.VideoListResponseBuilder builder = VideoListResponse.builder()
                    .videoId(video.getId())
                    .originalFileName(video.getOriginalFileName())
                    .fileSizeBytes(video.getFileSizeBytes())
                    .status(video.getStatus())
                    .processingType(video.getProcessingType())
                    .s3ThumbnailKey(video.getS3ThumbnailKey())
                    .uploadedAt(video.getCreatedDate())
                    .updatedAt(video.getLastModifiedDate());

            // 썸네일 URL 생성 (Public URL 방식 사용)
            if (video.getS3ThumbnailKey() != null) {
                log.info("Generating thumbnail URL for video {} with key: {}", video.getId(), video.getS3ThumbnailKey());
                String thumbnailUrl = awsS3Service.generateFileUrl(video.getS3ThumbnailKey());
                builder.thumbnailUrl(thumbnailUrl);
                log.info("Generated thumbnail URL for video {}: {}", video.getId(), thumbnailUrl);
            } else {
                log.info("No thumbnail key for video {}, skipping URL generation", video.getId());
            }

            // 스트리밍 관련 기능은 보류
            // TODO: 향후 스트리밍 기능 구현 시 추가
            // - generateStreamingUrl()
            // - generateDownloadUrl()
            // - urlExpiresAt 설정

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to generate thumbnail URL for video: {}, error: {}", video.getId(), e.getMessage());
            
            // URL 생성 실패 시에도 기본 정보는 제공
            return VideoListResponse.builder()
                    .videoId(video.getId())
                    .originalFileName(video.getOriginalFileName())
                    .fileSizeBytes(video.getFileSizeBytes())
                    .status(video.getStatus())
                    .processingType(video.getProcessingType())
                    .s3ThumbnailKey(video.getS3ThumbnailKey())
                    .uploadedAt(video.getCreatedDate())
                    .updatedAt(video.getLastModifiedDate())
                    .build();
        }
    }
}
