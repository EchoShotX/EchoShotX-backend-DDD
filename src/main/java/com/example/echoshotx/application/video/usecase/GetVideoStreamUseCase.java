package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import com.example.echoshotx.infrastructure.exception.payload.dto.ApiResponseDto;
import com.example.echoshotx.infrastructure.service.AwsS3Service;
import com.example.echoshotx.presentation.video.dto.response.VideoStreamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 영상 스트리밍을 위한 UseCase
 * 
 * 주요 책임:
 * 1. 영상 존재 여부 및 권한 검증
 * 2. S3 Pre-signed URL 생성
 * 3. 스트리밍 메타데이터 조합
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVideoStreamUseCase {

    private final VideoRepository videoRepository;
    private final AwsS3Service awsS3Service;

    /**
     * 영상 스트리밍 정보를 조회하고 Pre-signed URL을 생성합니다
     * 
     * @param videoId 영상 ID
     * @param member 요청한 사용자
     * @return 스트리밍 가능한 VideoStreamResponse
     */
    public VideoStreamResponse execute(Long videoId, Member member) {
        log.info("Generating streaming URL for video: {}, user: {}", videoId, member.getId());
        
        // 1. 영상 존재 여부 및 권한 확인
        Video video = validateVideoAccess(videoId, member);
        
        // 2. S3 객체 존재 여부 확인
        validateS3Object(video);
        
        // 3. Pre-signed URL 생성
        String streamingUrl = awsS3Service.generateStreamingUrl(video.getS3ProcessedKey());
        String thumbnailUrl = null;
        if (video.getS3ThumbnailKey() != null) {
            thumbnailUrl = awsS3Service.generateThumbnailUrl(video.getS3ThumbnailKey());
        }
        
        // 4. 만료 시간 계산 (1시간 후)
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        // 5. Response 생성
        VideoStreamResponse response = VideoStreamResponse.builder()
                .videoId(videoId.toString())
                .streamingUrl(streamingUrl)
                .thumbnailUrl(thumbnailUrl)
                .expiresAt(expiresAt)
                .contentType("video/mp4") // 기본값, 실제로는 메타데이터에서 추출
                .fileSizeBytes(video.getFileSizeBytes())
                .fileName(video.getOriginalFileName())
                .supportsRangeRequests(true) // S3는 Range 요청을 지원함
                .quality("HD") // 기본값, 실제로는 메타데이터에서 추출
                .build();
        
        log.info("Successfully generated streaming URL for video: {}, expires: {}", videoId, expiresAt);
        return response;
    }

    /**
     * 영상 접근 권한을 검증합니다
     */
    private Video validateVideoAccess(Long videoId, Member member) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.warn("Video not found: {}", videoId);
                    return new RuntimeException("Video not found"); // TODO: 적절한 예외로 변경
                });
        
        // TODO: 영상 소유자 확인 로직 추가
        // if (!video.getMember().getId().equals(member.getId())) {
        //     throw new UnauthorizedException("Access denied");
        // }
        
        return video;
    }

    /**
     * S3 객체 존재 여부를 확인합니다
     */
    private void validateS3Object(Video video) {
        if (video.getS3ProcessedKey() == null) {
            log.error("Video has no processed S3 key: {}", video.getId());
            throw new RuntimeException("Video not processed yet"); // TODO: 적절한 예외로 변경
        }
        
        if (!awsS3Service.doesObjectExist(video.getS3ProcessedKey())) {
            log.error("S3 object not found for key: {}", video.getS3ProcessedKey());
            throw new RuntimeException("Video file not found in S3"); // TODO: 적절한 예외로 변경
        }
    }
}
