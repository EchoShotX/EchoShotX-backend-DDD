package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.entity.Role;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.exception.VideoErrorStatus;
import com.example.echoshotx.video.domain.vo.ProcessedVideo;
import com.example.echoshotx.video.domain.vo.VideoFile;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.response.VideoDetailResponse;
import com.example.echoshotx.video.presentation.exception.VideoHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * GetVideoUseCase 단위 테스트
 *
 * <p>테스트 범위:
 * <ol>
 *   <li>정상적인 Video 조회 및 반환 (처리 완료된 상태)</li>
 *   <li>업로드 직후 Video 조회 (처리 전 상태)</li>
 *   <li>Video를 찾을 수 없는 경우</li>
 *   <li>Member가 Video 소유자가 아닌 경우</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetVideoUseCase 테스트")
class GetVideoUseCaseTest {

    @Mock
    private VideoAdaptor videoAdaptor;

    @InjectMocks
    private GetVideoUseCase getVideoUseCase;

    private Member testMember;
    private Member otherMember;
    private Long videoId;

    @BeforeEach
    void setUp() {
        videoId = 100L;

        testMember = Member.builder()
                .id(1L)
                .username("testuser@example.com")
                .email("testuser@example.com")
                .role(Role.USER)
                .currentCredits(1000)
                .build();

        otherMember = Member.builder()
                .id(2L)
                .username("otheruser@example.com")
                .email("otheruser@example.com")
                .role(Role.USER)
                .currentCredits(500)
                .build();
    }

    @Nested
    @DisplayName("execute 메서드 - 성공 케이스")
    class SuccessTest {

        @Test
        @DisplayName("성공: 처리 완료된 Video 조회 및 반환")
        void execute_Success_WhenVideoCompleted() {
            // Given
            Video completedVideo = Video.builder()
                    .id(videoId)
                    .memberId(1L)
                    .originalFile(
                            VideoFile.builder()
                                    .fileName("test-video.mp4")
                                    .fileSizeBytes(10_000_000L)
                                    .s3Key("videos/1/original/upload-id/test-video.mp4")
                                    .build())
                    .processedVideo(
                            ProcessedVideo.builder()
                                    .s3Key("videos/1/processed/upload-id/processed-test-video.mp4")
                                    .fileSizeBytes(15_000_000L)
                                    .build())
                    .processedMetadata(
                            VideoMetadata.builder()
                                    .durationSeconds(120.5)
                                    .width(1920)
                                    .height(1080)
                                    .codec("h264")
                                    .bitrate(5_000_000L)
                                    .frameRate(30.0)
                                    .build())
                    .status(VideoStatus.COMPLETED)
                    .processingType(ProcessingType.AI_UPSCALING)
                    .s3ThumbnailKey("videos/1/thumbnail/upload-id/thumbnail.jpg")
                    .build();

            given(videoAdaptor.queryById(videoId)).willReturn(completedVideo);

            // When
            VideoDetailResponse response = getVideoUseCase.execute(videoId, testMember);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(videoId);
            assertThat(response.getOriginalFileName()).isEqualTo("test-video.mp4");
            assertThat(response.getS3OriginalKey()).isEqualTo("videos/1/original/upload-id/test-video.mp4");
            assertThat(response.getS3ProcessedKey()).isEqualTo("videos/1/processed/upload-id/processed-test-video.mp4");
            assertThat(response.getS3ThumbnailKey()).isEqualTo("videos/1/thumbnail/upload-id/thumbnail.jpg");
            assertThat(response.getFileSizeBytes()).isEqualTo(15_000_000L);
            assertThat(response.getStatus()).isEqualTo(VideoStatus.COMPLETED);
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.AI_UPSCALING);
            assertThat(response.getMetadata()).isNotNull();
            assertThat(response.getMetadata().getDurationSeconds()).isEqualTo(120.5);
            assertThat(response.getMetadata().getWidth()).isEqualTo(1920);
            assertThat(response.getMetadata().getHeight()).isEqualTo(1080);
        }

        @Test
        @DisplayName("성공: 업로드 직후 Video 조회 (처리 전 상태 - UPLOAD_COMPLETED)")
        void execute_Success_WhenVideoUploadCompleted() {
            // Given
            Video uploadCompletedVideo = Video.builder()
                    .id(videoId)
                    .memberId(1L)
                    .originalFile(
                            VideoFile.builder()
                                    .fileName("test-video.mp4")
                                    .fileSizeBytes(10_000_000L)
                                    .s3Key("videos/1/original/upload-id/test-video.mp4")
                                    .build())
                    .processedVideo(ProcessedVideo.empty())  // 처리 전이므로 비어있음
                    .processedMetadata(null)  // 처리 전이므로 null
                    .status(VideoStatus.UPLOAD_COMPLETED)
                    .processingType(ProcessingType.AI_UPSCALING)
                    .s3ThumbnailKey(null)
                    .build();

            given(videoAdaptor.queryById(videoId)).willReturn(uploadCompletedVideo);

            // When
            VideoDetailResponse response = getVideoUseCase.execute(videoId, testMember);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(videoId);
            assertThat(response.getOriginalFileName()).isEqualTo("test-video.mp4");
            assertThat(response.getS3OriginalKey()).isEqualTo("videos/1/original/upload-id/test-video.mp4");
            assertThat(response.getStatus()).isEqualTo(VideoStatus.UPLOAD_COMPLETED);
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.AI_UPSCALING);
            // processedVideo가 empty()인 경우 s3ProcessedKey와 fileSizeBytes는 null일 수 있음
            assertThat(response.getS3ProcessedKey()).isNull();
            assertThat(response.getFileSizeBytes()).isNull();
            assertThat(response.getMetadata()).isNull();
            assertThat(response.getS3ThumbnailKey()).isNull();
        }

        @Test
        @DisplayName("성공: 업로드 직후 Video 조회 (처리 전 상태 - PENDING_UPLOAD)")
        void execute_Success_WhenVideoPendingUpload() {
            // Given
            Video pendingUploadVideo = Video.builder()
                    .id(videoId)
                    .memberId(1L)
                    .originalFile(
                            VideoFile.builder()
                                    .fileName("test-video.mp4")
                                    .fileSizeBytes(10_000_000L)
                                    .s3Key("videos/1/original/upload-id/test-video.mp4")
                                    .build())
                    .processedVideo(ProcessedVideo.empty())  // 처리 전이므로 비어있음
                    .processedMetadata(null)  // 처리 전이므로 null
                    .status(VideoStatus.PENDING_UPLOAD)
                    .processingType(ProcessingType.AI_UPSCALING)
                    .s3ThumbnailKey(null)
                    .build();

            given(videoAdaptor.queryById(videoId)).willReturn(pendingUploadVideo);

            // When
            VideoDetailResponse response = getVideoUseCase.execute(videoId, testMember);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(videoId);
            assertThat(response.getOriginalFileName()).isEqualTo("test-video.mp4");
            assertThat(response.getS3OriginalKey()).isEqualTo("videos/1/original/upload-id/test-video.mp4");
            assertThat(response.getStatus()).isEqualTo(VideoStatus.PENDING_UPLOAD);
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.AI_UPSCALING);
            // processedVideo가 empty()인 경우 s3ProcessedKey와 fileSizeBytes는 null일 수 있음
            assertThat(response.getS3ProcessedKey()).isNull();
            assertThat(response.getFileSizeBytes()).isNull();
            assertThat(response.getMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("execute 메서드 - 실패 케이스")
    class FailureTest {

        @Test
        @DisplayName("실패: Video를 찾을 수 없음")
        void execute_ThrowsException_WhenVideoNotFound() {
            // Given
            given(videoAdaptor.queryById(videoId))
                    .willThrow(new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> getVideoUseCase.execute(videoId, testMember))
                    .isInstanceOf(VideoHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", VideoErrorStatus.VIDEO_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: Member가 Video 소유자가 아님")
        void execute_ThrowsException_WhenMemberMismatch() {
            // Given
            Video video = Video.builder()
                    .id(videoId)
                    .memberId(1L)  // testMember의 ID
                    .originalFile(
                            VideoFile.builder()
                                    .fileName("test-video.mp4")
                                    .fileSizeBytes(10_000_000L)
                                    .s3Key("videos/1/original/upload-id/test-video.mp4")
                                    .build())
                    .processedVideo(ProcessedVideo.empty())
                    .status(VideoStatus.UPLOAD_COMPLETED)
                    .processingType(ProcessingType.AI_UPSCALING)
                    .build();

            given(videoAdaptor.queryById(videoId)).willReturn(video);

            // When & Then - otherMember (ID: 2L)가 video (memberId: 1L)를 조회하려고 시도
            assertThatThrownBy(() -> getVideoUseCase.execute(videoId, otherMember))
                    .isInstanceOf(VideoHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", VideoErrorStatus.VIDEO_MEMBER_MISMATCH);
        }
    }
}

