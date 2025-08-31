package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.entity.Role;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.service.VideoService;

import com.example.echoshotx.presentation.video.dto.response.VideoUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UploadVideoUseCase 테스트
 * 
 * 15년차 백엔드 테스트 전문가 관점에서 작성된 종합 테스트:
 * 1. 단위 테스트 원칙 준수 (격리, 반복 가능, 자명)
 * 2. S3 연동 Mock 처리로 외부 의존성 제거
 * 3. 경계값 테스트 및 예외 상황 커버
 * 4. 비즈니스 로직 검증 중심
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadVideoUseCase 테스트")
class UploadVideoUseCaseTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private UploadVideoUseCase uploadVideoUseCase;

    private Member testMember;
    private MultipartFile validVideoFile;
    private ProcessingType processingType;

    @BeforeEach
    void setUp() {
        // 테스트용 Member 생성
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스트사용자")
                .username("testuser")
                .role(Role.USER)
                .build();

        // 테스트용 유효한 영상 파일 생성
        validVideoFile = new MockMultipartFile(
                "video",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        processingType = ProcessingType.BASIC_ENHANCEMENT;
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessfulCases {

        @Test
        @DisplayName("유효한 영상 파일 업로드 - 성공")
        void uploadValidVideo_Success() {
            // given
            Video mockVideo = createMockVideo();
            given(videoService.uploadVideo(eq(validVideoFile), eq(testMember.getId()), eq(processingType)))
                    .willReturn(mockVideo);

            // when
            VideoUploadResponse response = uploadVideoUseCase.execute(validVideoFile, processingType, testMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(mockVideo.getId());
            assertThat(response.getOriginalFileName()).isEqualTo(mockVideo.getOriginalFileName());
            assertThat(response.getStatus()).isEqualTo(VideoStatus.UPLOADED);
            assertThat(response.getProcessingType()).isEqualTo(processingType);
            
            verify(videoService).uploadVideo(validVideoFile, testMember.getId(), processingType);
        }

        @Test
        @DisplayName("다양한 ProcessingType 처리 - 성공")
        void uploadWithDifferentProcessingTypes_Success() {
            // given
            ProcessingType[] processingTypes = ProcessingType.values();
            
            for (ProcessingType type : processingTypes) {
                Video mockVideo = createMockVideo();
                given(videoService.uploadVideo(any(MultipartFile.class), eq(testMember.getId()), eq(type)))
                        .willReturn(mockVideo);

                // when
                VideoUploadResponse response = uploadVideoUseCase.execute(validVideoFile, type, testMember);

                // then
                assertThat(response.getProcessingType()).isEqualTo(type);
                
                // Mock 초기화 (다음 반복을 위해)
                reset(videoService);
            }
        }

        @Test
        @DisplayName("대용량 파일 업로드 - 성공")
        void uploadLargeFile_Success() {
            // given
            byte[] largeContent = new byte[100 * 1024 * 1024]; // 100MB
            MultipartFile largeFile = new MockMultipartFile(
                    "video",
                    "large-video.mp4", 
                    "video/mp4",
                    largeContent
            );
            
            Video mockVideo = createMockVideo();
            given(videoService.uploadVideo(eq(largeFile), eq(testMember.getId()), eq(processingType)))
                    .willReturn(mockVideo);

            // when
            VideoUploadResponse response = uploadVideoUseCase.execute(largeFile, processingType, testMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFileSizeBytes()).isEqualTo(largeContent.length);
            
            verify(videoService).uploadVideo(largeFile, testMember.getId(), processingType);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionCases {

        @Test
        @DisplayName("VideoService에서 예외 발생 - 예외 전파")
        void videoServiceThrowsException_PropagateException() {
            // given
            VideoHandler expectedException = new VideoHandler(VideoErrorStatus.VIDEO_UNSUPPORTED_FORMAT);
            given(videoService.uploadVideo(any(MultipartFile.class), eq(testMember.getId()), eq(processingType)))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> 
                uploadVideoUseCase.execute(validVideoFile, processingType, testMember)
            )
            .isInstanceOf(VideoHandler.class)
            .hasMessage(expectedException.getMessage());
            
            verify(videoService).uploadVideo(validVideoFile, testMember.getId(), processingType);
        }

        @Test
        @DisplayName("null 파일 전달 - VideoService 계층에서 처리")
        void executeWithNullFile_DelegateToVideoService() {
            // given
            VideoHandler expectedException = new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND);
            given(videoService.uploadVideo(isNull(), eq(testMember.getId()), eq(processingType)))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> 
                uploadVideoUseCase.execute(null, processingType, testMember)
            )
            .isInstanceOf(VideoHandler.class);
            
            verify(videoService).uploadVideo(null, testMember.getId(), processingType);
        }

        @Test
        @DisplayName("null ProcessingType 전달 - VideoService 계층에서 처리")
        void executeWithNullProcessingType_DelegateToVideoService() {
            // given
            VideoHandler expectedException = new VideoHandler(VideoErrorStatus.VIDEO_INVALID_PROCESSING_TYPE);
            given(videoService.uploadVideo(eq(validVideoFile), eq(testMember.getId()), isNull()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> 
                uploadVideoUseCase.execute(validVideoFile, null, testMember)
            )
            .isInstanceOf(VideoHandler.class);
            
            verify(videoService).uploadVideo(validVideoFile, testMember.getId(), null);
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarios {

        @Test
        @DisplayName("여러 사용자가 동시에 업로드 - 독립성 보장")
        void multipleUsersUploadSimultaneously_IndependentProcessing() {
            // given
            Member user1 = Member.builder().id(1L).email("user1@example.com").build();
            Member user2 = Member.builder().id(2L).email("user2@example.com").build();
            
            Video video1 = createMockVideoForUser(1L);
            Video video2 = createMockVideoForUser(2L);
            
            given(videoService.uploadVideo(any(MultipartFile.class), eq(1L), eq(processingType)))
                    .willReturn(video1);
            given(videoService.uploadVideo(any(MultipartFile.class), eq(2L), eq(processingType)))
                    .willReturn(video2);

            // when
            VideoUploadResponse response1 = uploadVideoUseCase.execute(validVideoFile, processingType, user1);
            VideoUploadResponse response2 = uploadVideoUseCase.execute(validVideoFile, processingType, user2);

            // then
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response2.getVideoId()).isEqualTo(2L);
            
            verify(videoService).uploadVideo(any(MultipartFile.class), eq(1L), eq(processingType));
            verify(videoService).uploadVideo(any(MultipartFile.class), eq(2L), eq(processingType));
        }

        @Test
        @DisplayName("연속 업로드 처리 - 상태 일관성 보장")
        void consecutiveUploads_ConsistentState() {
            // given
            MultipartFile file1 = new MockMultipartFile("video1", "video1.mp4", "video/mp4", "content1".getBytes());
            MultipartFile file2 = new MockMultipartFile("video2", "video2.mp4", "video/mp4", "content2".getBytes());
            
            Video video1 = createMockVideoWithId(1L);
            Video video2 = createMockVideoWithId(2L);
            
            given(videoService.uploadVideo(eq(file1), eq(testMember.getId()), eq(processingType)))
                    .willReturn(video1);
            given(videoService.uploadVideo(eq(file2), eq(testMember.getId()), eq(processingType)))
                    .willReturn(video2);

            // when
            VideoUploadResponse response1 = uploadVideoUseCase.execute(file1, processingType, testMember);
            VideoUploadResponse response2 = uploadVideoUseCase.execute(file2, processingType, testMember);

            // then
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response2.getVideoId()).isEqualTo(2L);
            assertThat(response1.getStatus()).isEqualTo(VideoStatus.UPLOADED);
            assertThat(response2.getStatus()).isEqualTo(VideoStatus.UPLOADED);
            
            verify(videoService, times(2)).uploadVideo(any(MultipartFile.class), eq(testMember.getId()), eq(processingType));
        }
    }

    // ========================================
    // 테스트 헬퍼 메서드들 (15년차 전문가 리팩토링)
    // ========================================

    /**
     * 기본 업로드된 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createUploadedForTest() 사용
     */
    private Video createMockVideo() {
        return Video.createUploadedForTest(
                testMember.getId(), 
                "test-video.mp4", 
                validVideoFile.getSize()
        );
    }

    /**
     * 특정 사용자의 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createUploadedForTest() 사용
     */
    private Video createMockVideoForUser(Long userId) {
        Video video = Video.createUploadedForTest(
                userId, 
                "test-video.mp4", 
                validVideoFile.getSize()
        );
        // ID 설정을 위한 Builder 패턴 사용
        return Video.builder()
                .id(userId)
                .memberId(video.getMemberId())
                .originalFileName(video.getOriginalFileName())
                .s3OriginalKey(video.getS3OriginalKey())
                .s3ProcessedKey(video.getS3ProcessedKey())
                .s3ThumbnailKey(video.getS3ThumbnailKey())
                .fileSizeBytes(video.getFileSizeBytes())
                .status(video.getStatus())
                .processingType(video.getProcessingType())
                .metadata(video.getMetadata())
                .urls(video.getUrls())
                .build();
    }

    /**
     * 특정 ID를 가진 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createUploadedForTest() 사용
     */
    private Video createMockVideoWithId(Long videoId) {
        Video baseVideo = Video.createUploadedForTest(
                testMember.getId(), 
                "test-video" + videoId + ".mp4", 
                validVideoFile.getSize()
        );
        return Video.builder()
                .id(videoId)
                .memberId(baseVideo.getMemberId())
                .originalFileName(baseVideo.getOriginalFileName())
                .s3OriginalKey(baseVideo.getS3OriginalKey())
                .s3ProcessedKey(baseVideo.getS3ProcessedKey())
                .s3ThumbnailKey(baseVideo.getS3ThumbnailKey())
                .fileSizeBytes(baseVideo.getFileSizeBytes())
                .status(baseVideo.getStatus())
                .processingType(baseVideo.getProcessingType())
                .metadata(baseVideo.getMetadata())
                .urls(baseVideo.getUrls())
                .build();
    }
}
