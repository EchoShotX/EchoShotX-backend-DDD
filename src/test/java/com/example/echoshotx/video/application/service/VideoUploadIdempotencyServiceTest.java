package com.example.echoshotx.video.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.entity.VideoUploadIdempotencyRecord;
import com.example.echoshotx.video.infrastructure.persistence.VideoUploadIdempotencyRepository;
import com.example.echoshotx.video.presentation.dto.response.CompleteUploadResponse;
import com.example.echoshotx.video.presentation.exception.VideoHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoUploadIdempotencyServiceTest {

    @Mock
    private VideoUploadIdempotencyRepository repository;

    private VideoUploadIdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new VideoUploadIdempotencyService(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("성공: 동일 요청 해시이면 저장된 응답을 반환한다")
    void findSuccessResponse_ReturnsCachedResponse_WhenHashMatches() {
        CompleteUploadResponse response =
                CompleteUploadResponse.builder()
                        .videoId(10L)
                        .status(VideoStatus.QUEUED)
                        .message("영상 처리가 시작되었습니다.")
                        .sqsMessageId("msg-1")
                        .build();

        VideoUploadIdempotencyRecord record =
                VideoUploadIdempotencyRecord.create(
                        1L,
                        10L,
                        "idem-key",
                        "same-hash",
                        200,
                        "{\"videoId\":10,\"status\":\"QUEUED\",\"message\":\"영상 처리가 시작되었습니다.\",\"sqsMessageId\":\"msg-1\"}",
                        LocalDateTime.now().plusHours(1));

        given(repository.findByMemberIdAndVideoIdAndIdempotencyKey(1L, 10L, "idem-key"))
                .willReturn(Optional.of(record));

        Optional<CompleteUploadResponse> cached =
                service.findSuccessResponse(1L, 10L, "idem-key", "same-hash");

        assertThat(cached).isPresent();
        assertThat(cached.get().getVideoId()).isEqualTo(10L);
        assertThat(cached.get().getStatus()).isEqualTo(VideoStatus.QUEUED);
    }

    @Test
    @DisplayName("실패: 동일 멱등 키에 요청 해시가 다르면 예외가 발생한다")
    void findSuccessResponse_Throws_WhenHashDiffers() {
        VideoUploadIdempotencyRecord record =
                VideoUploadIdempotencyRecord.create(
                        1L,
                        10L,
                        "idem-key",
                        "old-hash",
                        200,
                        "{}",
                        LocalDateTime.now().plusHours(1));

        given(repository.findByMemberIdAndVideoIdAndIdempotencyKey(1L, 10L, "idem-key"))
                .willReturn(Optional.of(record));

        assertThatThrownBy(() -> service.findSuccessResponse(1L, 10L, "idem-key", "new-hash"))
                .isInstanceOf(VideoHandler.class);
    }
}
