package com.example.echoshotx.domain.video.service;

import com.example.echoshotx.domain.video.entity.ProcessingStatus;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.domain.video.validator.VideoValidator;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.infrastructure.ai.dto.response.VideoProcessingResponse;
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


}
