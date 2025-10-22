package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.infrastructure.annotation.usecase.UseCase;
import com.example.echoshotx.infrastructure.aws.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class InitiateVideoUploadUseCase {

    private final AwsS3Service awsS3Service;

    public Object execute() {

    }

}
