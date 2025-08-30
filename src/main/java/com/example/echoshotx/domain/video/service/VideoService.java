package com.example.echoshotx.domain.video.service;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.domain.video.validator.VideoValidator;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.infrastructure.exception.object.domain.S3Handler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
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
    private final VideoValidator videoValidator;

    private static final String VIDEO_UPLOAD_PATH = "videos/";
    //todo 상세 경로 설정

    /**
     * 영상 업로드 및 DB 저장
     */
    // todo s3, db 정합성 해결
    @Transactional
    public Video uploadVideo(MultipartFile file, Long memberId, ProcessingType processingType) {
        // VideoValidator를 사용하여 파일 검증
        videoValidator.validateVideoFile(file);
        
        // 1. S3에 파일 업로드
        String s3FileName = awsS3Service.uploadVideo(file, VIDEO_UPLOAD_PATH);
        String s3Key = VIDEO_UPLOAD_PATH + s3FileName;

        // 2. Video 엔티티 생성 및 저장
        Video video = Video.create(memberId, file, s3Key, processingType,
                createBasicMetadata(file));

        Video savedVideo = videoRepository.save(video);
        
        log.info("영상 업로드 완료 - VideoID: {}, S3Key: {}", savedVideo.getId(), s3Key);
        return savedVideo;
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
