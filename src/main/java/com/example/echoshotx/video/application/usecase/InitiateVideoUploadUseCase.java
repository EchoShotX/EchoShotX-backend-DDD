package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.video.application.dto.PresignedUploadUrlResponse;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.aws.service.AwsS3Service;
import com.example.echoshotx.video.presentation.dto.request.InitiateUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.InitiateUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class InitiateVideoUploadUseCase {

    private final VideoService videoService;
    private final AwsS3Service awsS3Service;

    public InitiateUploadResponse execute(InitiateUploadRequest request, Member member) {
        String uploadId = UUID.randomUUID().toString();
        String s3Key = generateS3Key(member.getId(), uploadId, request.getFileName());
        // Presigned URL 생성
        PresignedUploadUrlResponse urlResponse = awsS3Service.generateUploadUrl(
                s3Key,
                request.getContentType(),
                request.getFilesSizeBytes()
        );
        // Video Entity 생성
        Video video = videoService.uploadVideo(
                member.getId(), request.getFileName(),
                request.getFilesSizeBytes(), request.getProcessingType(),
                s3Key, uploadId, urlResponse.getExpiresAt()
        );

        return InitiateUploadResponse.from(
                video, urlResponse,
                uploadId, s3Key, request.getContentType(), request.getFilesSizeBytes()
        );
    }

    // 구조: videos/{memberId}/original/{uploadId}/{timestamp}_{fileName}
    private String generateS3Key(Long memberId, String uploadId, String fileName) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return String.format("videos/%d/original/%s/%s_%s",
                memberId, uploadId, timestamp, fileName);
    }

}
