package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.infrastructure.aws.service.AwsS3Service;
import com.example.echoshotx.presentation.video.dto.request.InitiateUploadRequest;
import com.example.echoshotx.presentation.video.dto.response.InitiateUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class InitiateVideoUploadUseCase {

    private final AwsS3Service awsS3Service;

    public InitiateUploadResponse execute(InitiateUploadRequest request) {
        // 파일명 검증
        // S3키 생성
        // Presigned URL 생성
        // Video Entity 생성
        return null;
    }

}
