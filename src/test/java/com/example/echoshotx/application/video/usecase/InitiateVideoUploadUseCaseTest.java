package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.video.application.dto.PresignedUploadUrlResponse;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.entity.Role;
import com.example.echoshotx.video.application.usecase.InitiateVideoUploadUseCase;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.vo.VideoFile;
import com.example.echoshotx.shared.aws.service.AwsS3Service;
import com.example.echoshotx.video.presentation.dto.request.InitiateUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.InitiateUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * InitiateVideoUploadUseCase 단위 테스트
 * 
 * 테스트 범위:
 * 1. Presigned URL 생성 및 응답 검증
 * 2. S3 Key 생성 로직 검증
 * 3. Video 엔티티 생성 검증
 * 4. 의존성 메서드 호출 검증
 * 5. Response 매핑 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InitiateVideoUploadUseCase 테스트")
class InitiateVideoUploadUseCaseTest {

    @Mock
    private VideoService videoService;

    @Mock
    private AwsS3Service awsS3Service;

    @InjectMocks
    private InitiateVideoUploadUseCase initiateVideoUploadUseCase;

    private Member testMember;
    private InitiateUploadRequest testRequest;
    private Video testVideo;
    private PresignedUploadUrlResponse testPresignedUrlResponse;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        testMember = Member.builder()
                .id(1L)
                .username("testuser@example.com")
                .nickname("테스트유저")
                .email("testuser@example.com")
                .role(Role.USER)
                .currentCredits(1000)
                .build();

        testRequest = createTestRequest(
                "test-video.mp4",
                10_000_000L,  // 10MB
                "video/mp4",
                ProcessingType.BASIC_ENHANCEMENT
        );

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        
        testPresignedUrlResponse = PresignedUploadUrlResponse.builder()
                .uploadUrl("https://test-bucket.s3.amazonaws.com/presigned-url")
                .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                .expiresAt(expiresAt)
                .contentType("video/mp4")
                .maxSizeBytes(10_000_000L)
                .build();

        testVideo = Video.builder()
                .id(100L)
                .memberId(1L)
                .originalFile(VideoFile.builder()
                        .fileName("test-video.mp4")
                        .fileSizeBytes(10_000_000L)
                        .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                        .build())
                .status(VideoStatus.PENDING_UPLOAD)
                .processingType(ProcessingType.BASIC_ENHANCEMENT)
                .uploadId("upload-id")
                .presignedUrlExpiresAt(expiresAt)
                .retryCount(0)
                .build();
    }

    @Nested
    @DisplayName("execute 메서드 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("성공: 정상적인 업로드 초기화 요청 처리")
        void execute_Success_WhenValidRequest() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(), 
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(100L);
            assertThat(response.getUploadUrl()).isEqualTo(testPresignedUrlResponse.getUploadUrl());
            assertThat(response.getS3Key()).contains("videos/1/original/");
            assertThat(response.getS3Key()).contains("test-video.mp4");
            assertThat(response.getExpiresAt()).isNotNull();
            assertThat(response.getContentType()).isEqualTo("video/mp4");
            assertThat(response.getMaxSizeBytes()).isEqualTo(10_000_000L);
            assertThat(response.getUploadId()).isNotNull();
        }

        @Test
        @DisplayName("성공: AwsS3Service.generateUploadUrl이 올바른 파라미터로 호출됨")
        void execute_CallsAwsS3Service_WithCorrectParameters() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);

            // When
            initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            verify(awsS3Service).generateUploadUrl(
                    s3KeyCaptor.capture(),
                    contentTypeCaptor.capture(),
                    fileSizeCaptor.capture()
            );

            String capturedS3Key = s3KeyCaptor.getValue();
            assertThat(capturedS3Key).startsWith("videos/1/original/");
            assertThat(capturedS3Key).endsWith("test-video.mp4");
            assertThat(contentTypeCaptor.getValue()).isEqualTo("video/mp4");
            assertThat(fileSizeCaptor.getValue()).isEqualTo(10_000_000L);
        }

        @Test
        @DisplayName("성공: VideoService.uploadVideo가 올바른 파라미터로 호출됨")
        void execute_CallsVideoService_WithCorrectParameters() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<ProcessingType> processingTypeCaptor = ArgumentCaptor.forClass(ProcessingType.class);
            ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> uploadIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            // When
            initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            verify(videoService).uploadVideo(
                    memberIdCaptor.capture(),
                    fileNameCaptor.capture(),
                    fileSizeCaptor.capture(),
                    processingTypeCaptor.capture(),
                    s3KeyCaptor.capture(),
                    uploadIdCaptor.capture(),
                    expiresAtCaptor.capture()
            );

            assertThat(memberIdCaptor.getValue()).isEqualTo(1L);
            assertThat(fileNameCaptor.getValue()).isEqualTo("test-video.mp4");
            assertThat(fileSizeCaptor.getValue()).isEqualTo(10_000_000L);
            assertThat(processingTypeCaptor.getValue()).isEqualTo(ProcessingType.BASIC_ENHANCEMENT);
            assertThat(s3KeyCaptor.getValue()).contains("videos/1/original/");
            assertThat(uploadIdCaptor.getValue()).isNotEmpty();
            assertThat(expiresAtCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("성공: 메서드 호출 순서가 올바름 (S3 URL 생성 -> Video 생성)")
        void execute_CallsMethodsInCorrectOrder() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            var inOrder = inOrder(awsS3Service, videoService);
            inOrder.verify(awsS3Service).generateUploadUrl(anyString(), anyString(), anyLong());
            inOrder.verify(videoService).uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("S3 Key 생성 로직 테스트")
    class S3KeyGenerationTest {

        @Test
        @DisplayName("성공: S3 Key가 올바른 형식으로 생성됨 (videos/{memberId}/original/{uploadId}/{timestamp}_{fileName})")
        void s3Key_GeneratedWithCorrectFormat() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);

            // When
            initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            verify(awsS3Service).generateUploadUrl(s3KeyCaptor.capture(), anyString(), anyLong());

            String s3Key = s3KeyCaptor.getValue();
            
            // 형식: videos/{memberId}/original/{uploadId}/{timestamp}_{fileName}
            assertThat(s3Key).matches("videos/\\d+/original/[a-f0-9-]+/\\d{14}_test-video\\.mp4");
            assertThat(s3Key).startsWith("videos/1/original/");
            assertThat(s3Key).contains("test-video.mp4");
        }

        @Test
        @DisplayName("성공: 다른 회원 ID로 S3 Key가 다르게 생성됨")
        void s3Key_GeneratedDifferently_ForDifferentMembers() {
            // Given
            Member anotherMember = Member.builder()
                    .id(2L)
                    .username("another@example.com")
                    .nickname("다른유저")
                    .role(Role.USER)
                    .currentCredits(1000)
                    .build();

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            ArgumentCaptor<String> s3KeyCaptor1 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> s3KeyCaptor2 = ArgumentCaptor.forClass(String.class);

            // When
            initiateVideoUploadUseCase.execute(testRequest, testMember);
            verify(awsS3Service).generateUploadUrl(s3KeyCaptor1.capture(), anyString(), anyLong());

            initiateVideoUploadUseCase.execute(testRequest, anotherMember);
            verify(awsS3Service, times(2)).generateUploadUrl(s3KeyCaptor2.capture(), anyString(), anyLong());

            // Then
            String s3Key1 = s3KeyCaptor1.getValue();
            String s3Key2 = s3KeyCaptor2.getAllValues().get(1);

            assertThat(s3Key1).startsWith("videos/1/original/");
            assertThat(s3Key2).startsWith("videos/2/original/");
            assertThat(s3Key1).isNotEqualTo(s3Key2);
        }

        @Test
        @DisplayName("성공: 매번 다른 uploadId가 생성됨")
        void uploadId_GeneratedUniquely() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response1 = initiateVideoUploadUseCase.execute(testRequest, testMember);
            InitiateUploadResponse response2 = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response1.getUploadId()).isNotEmpty();
            assertThat(response2.getUploadId()).isNotEmpty();
            assertThat(response1.getUploadId()).isNotEqualTo(response2.getUploadId());
        }
    }

    @Nested
    @DisplayName("Response 매핑 테스트")
    class ResponseMappingTest {

        @Test
        @DisplayName("성공: Response에 모든 필수 필드가 올바르게 매핑됨")
        void response_ContainsAllRequiredFields() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response.getVideoId()).isNotNull();
            assertThat(response.getUploadId()).isNotNull();
            assertThat(response.getUploadUrl()).isNotNull();
            assertThat(response.getS3Key()).isNotNull();
            assertThat(response.getExpiresAt()).isNotNull();
            assertThat(response.getContentType()).isNotNull();
            assertThat(response.getMaxSizeBytes()).isNotNull();
        }

        @Test
        @DisplayName("성공: Video ID가 Response에 올바르게 매핑됨")
        void response_ContainsCorrectVideoId() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response.getVideoId()).isEqualTo(testVideo.getId());
        }

        @Test
        @DisplayName("성공: Presigned URL이 Response에 올바르게 매핑됨")
        void response_ContainsCorrectPresignedUrl() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response.getUploadUrl()).isEqualTo(testPresignedUrlResponse.getUploadUrl());
        }

        @Test
        @DisplayName("성공: 만료 시간이 Response에 올바르게 매핑됨")
        void response_ContainsCorrectExpiresAt() {
            // Given
            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response.getExpiresAt())
                    .isCloseTo(testPresignedUrlResponse.getExpiresAt(), within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("다양한 파일 타입 테스트")
    class DifferentFileTypesTest {

        @Test
        @DisplayName("성공: MP4 파일 업로드 초기화")
        void execute_Success_WithMp4File() {
            // Given
            InitiateUploadRequest mp4Request = createTestRequest(
                    "video.mp4", 15_000_000L, "video/mp4", ProcessingType.BASIC_ENHANCEMENT
            );
            
            given(awsS3Service.generateUploadUrl(anyString(), eq("video/mp4"), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), eq("video.mp4"), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(mp4Request, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), eq("video/mp4"), anyLong());
        }

        @Test
        @DisplayName("성공: MOV 파일 업로드 초기화")
        void execute_Success_WithMovFile() {
            // Given
            InitiateUploadRequest movRequest = createTestRequest(
                    "video.mov", 20_000_000L, "video/quicktime", ProcessingType.AI_UPSCALING
            );
            
            given(awsS3Service.generateUploadUrl(anyString(), eq("video/quicktime"), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), eq("video.mov"), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(movRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), eq("video/quicktime"), anyLong());
        }

        @Test
        @DisplayName("성공: AVI 파일 업로드 초기화")
        void execute_Success_WithAviFile() {
            // Given
            InitiateUploadRequest aviRequest = createTestRequest(
                    "video.avi", 25_000_000L, "video/x-msvideo", ProcessingType.BASIC_ENHANCEMENT
            );
            
            given(awsS3Service.generateUploadUrl(anyString(), eq("video/x-msvideo"), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), eq("video.avi"), anyLong(),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(aviRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), eq("video/x-msvideo"), anyLong());
        }
    }

    @Nested
    @DisplayName("다양한 처리 타입 테스트")
    class DifferentProcessingTypesTest {

        @Test
        @DisplayName("성공: BASIC_ENHANCEMENT 처리 타입으로 업로드 초기화")
        void execute_Success_WithBasicEnhancement() {
            // Given
            testRequest = createTestRequest(
                    "video.mp4", 10_000_000L, "video/mp4", ProcessingType.BASIC_ENHANCEMENT
            );

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    eq(ProcessingType.BASIC_ENHANCEMENT), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(videoService).uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    eq(ProcessingType.BASIC_ENHANCEMENT), anyString(), anyString(), any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("성공: AI_UPSCALING 처리 타입으로 업로드 초기화")
        void execute_Success_WithAiUpscaling() {
            // Given
            testRequest = createTestRequest(
                    "video.mp4", 10_000_000L, "video/mp4", ProcessingType.AI_UPSCALING
            );

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), anyLong()))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    eq(ProcessingType.AI_UPSCALING), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(testRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(videoService).uploadVideo(
                    anyLong(), anyString(), anyLong(),
                    eq(ProcessingType.AI_UPSCALING), anyString(), anyString(), any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("파일 크기 경계값 테스트")
    class FileSizeBoundaryTest {

        @Test
        @DisplayName("성공: 최소 크기(1 byte) 파일 업로드 초기화")
        void execute_Success_WithMinimumFileSize() {
            // Given
            InitiateUploadRequest smallRequest = createTestRequest(
                    "tiny.mp4", 1L, "video/mp4", ProcessingType.BASIC_ENHANCEMENT
            );

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), eq(1L)))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), eq(1L),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(smallRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), anyString(), eq(1L));
        }

        @Test
        @DisplayName("성공: 최대 크기(500MB) 파일 업로드 초기화")
        void execute_Success_WithMaximumFileSize() {
            // Given
            long maxSize = 500L * 1024 * 1024; // 500MB
            InitiateUploadRequest largeRequest = createTestRequest(
                    "large.mp4", maxSize, "video/mp4", ProcessingType.AI_UPSCALING
            );

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), eq(maxSize)))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), eq(maxSize),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(largeRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), anyString(), eq(maxSize));
        }

        @Test
        @DisplayName("성공: 일반적인 크기(50MB) 파일 업로드 초기화")
        void execute_Success_WithNormalFileSize() {
            // Given
            long normalSize = 50L * 1024 * 1024; // 50MB
            InitiateUploadRequest normalRequest = createTestRequest(
                    "normal.mp4", normalSize, "video/mp4", ProcessingType.BASIC_ENHANCEMENT
            );

            given(awsS3Service.generateUploadUrl(anyString(), anyString(), eq(normalSize)))
                    .willReturn(testPresignedUrlResponse);
            given(videoService.uploadVideo(
                    anyLong(), anyString(), eq(normalSize),
                    any(ProcessingType.class), anyString(), anyString(), any(LocalDateTime.class)
            )).willReturn(testVideo);

            // When
            InitiateUploadResponse response = initiateVideoUploadUseCase.execute(normalRequest, testMember);

            // Then
            assertThat(response).isNotNull();
            verify(awsS3Service).generateUploadUrl(anyString(), anyString(), eq(normalSize));
        }
    }

    // ========== Helper Methods ==========

    /**
     * 테스트용 InitiateUploadRequest 생성 헬퍼 메서드
     */
    private InitiateUploadRequest createTestRequest(
            String fileName,
            Long fileSize,
            String contentType,
            ProcessingType processingType
    ) {
        InitiateUploadRequest request = new InitiateUploadRequest();
        
        // Reflection을 사용하여 private 필드 설정
        try {
            java.lang.reflect.Field fileNameField = InitiateUploadRequest.class.getDeclaredField("fileName");
            fileNameField.setAccessible(true);
            fileNameField.set(request, fileName);

            java.lang.reflect.Field fileSizeField = InitiateUploadRequest.class.getDeclaredField("filesSizeBytes");
            fileSizeField.setAccessible(true);
            fileSizeField.set(request, fileSize);

            java.lang.reflect.Field contentTypeField = InitiateUploadRequest.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(request, contentType);

            java.lang.reflect.Field processingTypeField = InitiateUploadRequest.class.getDeclaredField("processingType");
            processingTypeField.setAccessible(true);
            processingTypeField.set(request, processingType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test request", e);
        }

        return request;
    }
}

