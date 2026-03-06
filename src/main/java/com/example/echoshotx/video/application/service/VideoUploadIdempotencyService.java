package com.example.echoshotx.video.application.service;

import com.example.echoshotx.video.domain.entity.VideoUploadIdempotencyRecord;
import com.example.echoshotx.video.domain.exception.VideoErrorStatus;
import com.example.echoshotx.video.infrastructure.persistence.VideoUploadIdempotencyRepository;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.CompleteUploadResponse;
import com.example.echoshotx.video.presentation.exception.VideoHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoUploadIdempotencyService {

    private static final int SUCCESS_STATUS = 200;
    private static final long DEFAULT_TTL_HOURS = 24L;

    private final VideoUploadIdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public String createRequestHash(Long videoId, CompleteUploadRequest request) {
        String payload =
                videoId
                        + ":"
                        + request.getDurationSeconds()
                        + ":"
                        + request.getWidth()
                        + ":"
                        + request.getHeight()
                        + ":"
                        + request.getCodec()
                        + ":"
                        + request.getBitrate()
                        + ":"
                        + request.getFrameRate();
        return sha256(payload);
    }

    public Optional<CompleteUploadResponse> findSuccessResponse(
            Long memberId, Long videoId, String idempotencyKey, String requestHash) {
        Optional<VideoUploadIdempotencyRecord> optionalRecord =
                repository.findByMemberIdAndVideoIdAndIdempotencyKey(memberId, videoId, idempotencyKey);

        if (optionalRecord.isEmpty()) {
            return Optional.empty();
        }

        VideoUploadIdempotencyRecord record = optionalRecord.get();
        if (record.isExpired(LocalDateTime.now())) {
            return Optional.empty();
        }

        if (!record.getRequestHash().equals(requestHash)) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_IDEMPOTENCY_KEY_CONFLICT);
        }

        if (record.getResponseStatus() != SUCCESS_STATUS) {
            return Optional.empty();
        }

        return Optional.of(readResponse(record.getResponseBody()));
    }

    @Transactional
    public void saveSuccessResponse(
            Long memberId,
            Long videoId,
            String idempotencyKey,
            String requestHash,
            CompleteUploadResponse response) {
        String responseBody = writeResponse(response);
        VideoUploadIdempotencyRecord record =
                VideoUploadIdempotencyRecord.create(
                        memberId,
                        videoId,
                        idempotencyKey,
                        requestHash,
                        SUCCESS_STATUS,
                        responseBody,
                        LocalDateTime.now().plusHours(DEFAULT_TTL_HOURS));

        try {
            repository.save(record);
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "Idempotency record already exists. memberId={}, videoId={}, key={}",
                    memberId,
                    videoId,
                    idempotencyKey);
            repository.findByMemberIdAndVideoIdAndIdempotencyKey(memberId, videoId, idempotencyKey)
                    .ifPresent(existing -> {
                        if (!existing.getRequestHash().equals(requestHash)) {
                            throw new VideoHandler(VideoErrorStatus.VIDEO_IDEMPOTENCY_KEY_CONFLICT);
                        }
                    });
        }
    }

    private String writeResponse(CompleteUploadResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_IDEMPOTENCY_SERIALIZATION_FAILED);
        }
    }

    private CompleteUploadResponse readResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, CompleteUploadResponse.class);
        } catch (JsonProcessingException e) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_IDEMPOTENCY_DESERIALIZATION_FAILED);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_IDEMPOTENCY_HASH_FAILED);
        }
    }
}
