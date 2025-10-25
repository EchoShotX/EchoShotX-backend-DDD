package com.example.echoshotx.domain.video.service;

import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.domain.video.validator.VideoValidator;
import com.example.echoshotx.infrastructure.aws.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
