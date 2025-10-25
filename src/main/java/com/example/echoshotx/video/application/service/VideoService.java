package com.example.echoshotx.video.application.service;

import com.example.echoshotx.video.application.validator.VideoValidator;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.infrastructure.persistence.VideoRepository;
import com.example.echoshotx.shared.aws.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final AwsS3Service awsS3Service;
    private final VideoValidator videoValidator;

    public Video uploadVideo(Long memberId, String fileName, long fileSize,
                             ProcessingType processingType, String uploadId, String s3Key, LocalDateTime expiresAt) {
        Video video = Video.createForPresignedUpload(
                memberId,
                fileName,
                fileSize,
                processingType,
                s3Key,
                uploadId,
                expiresAt
        );
        return videoRepository.save(video);

    }

}
