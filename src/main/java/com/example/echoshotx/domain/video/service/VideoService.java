package com.example.echoshotx.domain.video.service;

import com.example.echoshotx.domain.video.entity.ProcessingStatus;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.domain.video.validator.VideoValidator;
import com.example.echoshotx.infrastructure.aws.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final AwsS3Service awsS3Service;
    private final VideoValidator videoValidator;


}
