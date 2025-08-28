package com.example.echoshotx.domain.video.service;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.infrastructure.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final AwsS3Service awsS3Service;

    private static final String VIDEO_UPLOAD_PATH = "videos/";

    /**
     * 영상 업로드 및 DB 저장
     */
    @Transactional
    public Video uploadVideo(MultipartFile file, Long memberId, ProcessingType processingType) {
        log.info("영상 업로드 시작 - 파일명: {}, 사용자ID: {}, 처리타입: {}", 
                file.getOriginalFilename(), memberId, processingType);

        // 1. S3에 파일 업로드
        String s3FileName = awsS3Service.uploadVideo(file, VIDEO_UPLOAD_PATH);
        String s3Key = VIDEO_UPLOAD_PATH + s3FileName;

        // 2. Video 엔티티 생성 및 저장
        Video video = Video.builder()
                .memberId(memberId)
                .originalFileName(file.getOriginalFilename())
                .s3OriginalKey(s3Key)
                .fileSizeBytes(file.getSize())
                .status(VideoStatus.UPLOADED)
                .processingType(processingType)
                .metadata(createBasicMetadata(file))
                .build();

        Video savedVideo = videoRepository.save(video);
        
        log.info("영상 업로드 완료 - VideoID: {}, S3Key: {}", savedVideo.getId(), s3Key);
        return savedVideo;
    }

    /**
     * 영상 ID로 조회
     */
    public Video findById(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("영상을 찾을 수 없습니다. ID: " + videoId));
    }

    /**
     * 사용자별 영상 목록 조회
     */
    public List<Video> findByMemberId(Long memberId) {
        return videoRepository.findByMemberIdOrderByCreatedDateDesc(memberId);
    }

    /**
     * 기본 메타데이터 생성 (향후 FFmpeg 연동 시 확장 예정)
     */
    private VideoMetadata createBasicMetadata(MultipartFile file) {
        return new VideoMetadata(
                null,  // durationSeconds - 향후 FFmpeg로 추출
                null,  // width - 향후 FFmpeg로 추출  
                null,  // height - 향후 FFmpeg로 추출
                null,  // codec - 향후 FFmpeg로 추출
                null,  // bitrate - 향후 FFmpeg로 추출
                null   // frameRate - 향후 FFmpeg로 추출
        );
    }
}
