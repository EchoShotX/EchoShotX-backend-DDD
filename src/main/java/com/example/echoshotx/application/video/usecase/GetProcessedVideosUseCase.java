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

import java.time.LocalDateTime;
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
        
        // 각 영상에 대해 Pre-signed URL 생성
        return processedVideos.stream()
                .map(this::enrichWithUrls)
                .toList();
    }

    /**
     * 영상 정보에 Pre-signed URL들을 추가합니다
     */
    private VideoListResponse enrichWithUrls(Video video) {
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

            // URL 만료 시간 설정 (1시간 후)
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

            // 썸네일 URL 생성
            if (video.getS3ThumbnailKey() != null) {
                String thumbnailUrl = awsS3Service.generateThumbnailUrl(video.getS3ThumbnailKey());
                builder.thumbnailUrl(thumbnailUrl);
            }

            // 스트리밍 URL 생성 (AI 처리된 영상)
            if (video.getS3ProcessedKey() != null) {
                String streamingUrl = awsS3Service.generateStreamingUrl(video.getS3ProcessedKey());
                builder.streamingUrl(streamingUrl);
            }

            // 다운로드 URL 생성 (원본 영상)
            if (video.getS3OriginalKey() != null) {
                String downloadUrl = awsS3Service.generateDownloadUrl(video.getS3OriginalKey());
                builder.downloadUrl(downloadUrl);
            }

            builder.urlExpiresAt(expiresAt);

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to generate URLs for video: {}, error: {}", video.getId(), e.getMessage());
            
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
