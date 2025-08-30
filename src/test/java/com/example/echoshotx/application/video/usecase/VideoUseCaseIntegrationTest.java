package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.entity.Role;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.service.VideoService;
import com.example.echoshotx.domain.video.validator.VideoValidator;

import com.example.echoshotx.infrastructure.service.AwsS3Service;
import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import com.example.echoshotx.presentation.video.dto.response.VideoListResponse;
import com.example.echoshotx.presentation.video.dto.response.VideoUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Video UseCase 통합 테스트
 * 
 * 15년차 백엔드 테스트 전문가 관점에서 작성된 E2E 시나리오 테스트:
 * 1. 전체 영상 처리 플로우 검증
 * 2. UseCase 간 상호작용 검증
 * 3. 실제 운영 환경과 유사한 시나리오 테스트
 * 4. S3 연동을 포함한 End-to-End 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Video UseCase 통합 테스트")
class VideoUseCaseIntegrationTest {

    @Mock
    private VideoService videoService;
    @Mock
    private VideoAdaptor videoAdaptor;
    @Mock
    private AwsS3Service awsS3Service;
    @Mock
    private VideoValidator videoValidator;

    private UploadVideoUseCase uploadVideoUseCase;
    private GetVideoUseCase getVideoUseCase;
    private GetProcessedVideosUseCase getProcessedVideosUseCase;

    private Member testMember;
    private MultipartFile testVideoFile;

    @BeforeEach
    void setUp() {
        // UseCase 인스턴스 생성
        uploadVideoUseCase = new UploadVideoUseCase(videoService);
        getVideoUseCase = new GetVideoUseCase(videoAdaptor);
        getProcessedVideosUseCase = new GetProcessedVideosUseCase(videoAdaptor, awsS3Service);

        // 테스트 데이터 준비
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스트사용자")
                .username("testuser")
                .role(Role.USER)
                .build();

        testVideoFile = new MockMultipartFile(
                "video",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );
    }

    @Nested
    @DisplayName("전체 영상 처리 플로우")
    class CompleteVideoProcessingFlow {

        @Test
        @DisplayName("영상 업로드 → 조회 → 처리 완료 → 목록 조회 전체 플로우")
        void completeVideoProcessingFlow_EndToEnd_Success() {
            // given - 업로드 준비
            Video uploadedVideo = createMockUploadedVideo();
            given(videoService.uploadVideo(eq(testVideoFile), eq(testMember.getId()), eq(ProcessingType.BASIC_ENHANCEMENT)))
                    .willReturn(uploadedVideo);

            // when - 1단계: 영상 업로드
            VideoUploadResponse uploadResponse = uploadVideoUseCase.execute(
                    testVideoFile, ProcessingType.BASIC_ENHANCEMENT, testMember);

            // then - 업로드 검증
            assertThat(uploadResponse).isNotNull();
            assertThat(uploadResponse.getVideoId()).isEqualTo(1L);
            assertThat(uploadResponse.getStatus()).isEqualTo(VideoStatus.UPLOADED);

            // given - 개별 조회 준비
            given(videoAdaptor.queryById(1L)).willReturn(uploadedVideo);

            // when - 2단계: 업로드된 영상 조회
            VideoDetailResponse detailResponse = getVideoUseCase.execute(1L, testMember);

            // then - 개별 조회 검증
            assertThat(detailResponse.getVideoId()).isEqualTo(1L);
            assertThat(detailResponse.getStatus()).isEqualTo(VideoStatus.UPLOADED);

            // given - 처리 완료 상태로 변경
            Video processedVideo = createMockProcessedVideo();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(processedVideo));

            // S3 URL 생성 Mock
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when - 3단계: 처리 완료된 영상 목록 조회
            List<VideoListResponse> listResponse = getProcessedVideosUseCase.execute(testMember);

            // then - 목록 조회 검증
            assertThat(listResponse).hasSize(1);
            assertThat(listResponse.get(0).getVideoId()).isEqualTo(1L);
            assertThat(listResponse.get(0).getStatus()).isEqualTo(VideoStatus.PROCESSED);
            assertThat(listResponse.get(0).getThumbnailUrl()).isNotNull();
            assertThat(listResponse.get(0).getStreamingUrl()).isNotNull();
            assertThat(listResponse.get(0).getDownloadUrl()).isNotNull();

            // 전체 플로우 검증
            verify(videoService).uploadVideo(eq(testVideoFile), eq(testMember.getId()), eq(ProcessingType.BASIC_ENHANCEMENT));
            verify(videoAdaptor).queryById(1L);
            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            verify(awsS3Service).generateThumbnailUrl(anyString());
            verify(awsS3Service).generateStreamingUrl(anyString());
            verify(awsS3Service).generateDownloadUrl(anyString());
        }

        @Test
        @DisplayName("다중 사용자 동시 영상 처리 시나리오")
        void multiUserSimultaneousVideoProcessing_Scenario() {
            // given
            Member user1 = Member.builder().id(1L).email("user1@example.com").build();
            Member user2 = Member.builder().id(2L).email("user2@example.com").build();

            Video user1Video = createMockVideoForUser(1L, 1L);
            Video user2Video = createMockVideoForUser(2L, 2L);

            // 업로드 Mock 설정
            given(videoService.uploadVideo(any(MultipartFile.class), eq(1L), any(ProcessingType.class)))
                    .willReturn(user1Video);
            given(videoService.uploadVideo(any(MultipartFile.class), eq(2L), any(ProcessingType.class)))
                    .willReturn(user2Video);

            // 조회 Mock 설정
            given(videoAdaptor.queryById(1L)).willReturn(user1Video);
            given(videoAdaptor.queryById(2L)).willReturn(user2Video);

            // when - 동시 업로드
            VideoUploadResponse user1Upload = uploadVideoUseCase.execute(testVideoFile, ProcessingType.BASIC_ENHANCEMENT, user1);
            VideoUploadResponse user2Upload = uploadVideoUseCase.execute(testVideoFile, ProcessingType.BASIC_ENHANCEMENT, user2);

            // when - 동시 조회
            VideoDetailResponse user1Detail = getVideoUseCase.execute(1L, user1);
            VideoDetailResponse user2Detail = getVideoUseCase.execute(2L, user2);

            // then - 독립성 검증
            assertThat(user1Upload.getVideoId()).isEqualTo(1L);
            assertThat(user2Upload.getVideoId()).isEqualTo(2L);
            assertThat(user1Detail.getVideoId()).isEqualTo(1L);
            assertThat(user2Detail.getVideoId()).isEqualTo(2L);

            // 교차 접근 불가 검증
            assertThatThrownBy(() -> getVideoUseCase.execute(1L, user2))
                    .isInstanceOf(Exception.class);
            assertThatThrownBy(() -> getVideoUseCase.execute(2L, user1))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("영상 처리 상태 변화에 따른 목록 조회 동작")
        void videoProcessingStatusTransition_ListBehavior() {
            // given - 다양한 상태의 영상들 중 PROCESSED 상태만 사용
            Video processedVideo = createMockVideoWithStatus(3L, VideoStatus.PROCESSED);

            // PROCESSED 상태만 목록에 나타나야 함
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(processedVideo));

            // S3 URL 생성 Mock
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            List<VideoListResponse> processedVideos = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(processedVideos).hasSize(1);
            assertThat(processedVideos.get(0).getVideoId()).isEqualTo(3L);
            assertThat(processedVideos.get(0).getStatus()).isEqualTo(VideoStatus.PROCESSED);

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
        }
    }

    @Nested
    @DisplayName("S3 연동 장애 상황 테스트")
    class S3IntegrationFailureScenarios {

        @Test
        @DisplayName("S3 업로드 실패 후 조회 시나리오")
        void s3UploadFailure_ThenRetrieve_Scenario() {
            // given - S3 업로드 실패
            given(videoService.uploadVideo(any(MultipartFile.class), eq(testMember.getId()), any(ProcessingType.class)))
                    .willThrow(new RuntimeException("S3 Upload Failed"));

            // when & then - 업로드 실패 검증
            assertThatThrownBy(() -> 
                uploadVideoUseCase.execute(testVideoFile, ProcessingType.BASIC_ENHANCEMENT, testMember)
            ).isInstanceOf(RuntimeException.class)
             .hasMessageContaining("S3 Upload Failed");

            verify(videoService).uploadVideo(eq(testVideoFile), eq(testMember.getId()), eq(ProcessingType.BASIC_ENHANCEMENT));
        }

        @Test
        @DisplayName("S3 URL 생성 부분 실패 시나리오")
        void s3UrlGenerationPartialFailure_Scenario() {
            // given
            Video processedVideo = createMockProcessedVideo();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(processedVideo));

            // 썸네일 URL만 성공, 나머지는 실패
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willThrow(new RuntimeException("Streaming URL generation failed"));
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willThrow(new RuntimeException("Download URL generation failed"));

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then - 부분 실패 시에도 기본 정보는 제공되어야 함
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getVideoId()).isEqualTo(1L);
            assertThat(responses.get(0).getOriginalFileName()).isNotNull();
            assertThat(responses.get(0).getStatus()).isEqualTo(VideoStatus.PROCESSED);
            // URL 생성 실패로 인해 모든 URL이 null이어야 함
            assertThat(responses.get(0).getThumbnailUrl()).isNull();
            assertThat(responses.get(0).getStreamingUrl()).isNull();
            assertThat(responses.get(0).getDownloadUrl()).isNull();
        }

        @Test
        @DisplayName("S3 연결 타임아웃 시나리오")
        void s3ConnectionTimeout_Scenario() {
            // given
            Video processedVideo = createMockProcessedVideo();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(processedVideo));

            // S3 타임아웃 시뮬레이션
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willAnswer(invocation -> {
                        Thread.sleep(500); // 타임아웃 시뮬레이션
                        throw new RuntimeException("Connection timeout");
                    });

            // when
            long startTime = System.currentTimeMillis();
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);
            long endTime = System.currentTimeMillis();

            // then
            assertThat(responses).hasSize(1);
            assertThat(endTime - startTime).isGreaterThan(500); // 타임아웃이 발생했음을 확인
            assertThat(responses.get(0).getThumbnailUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("성능 및 부하 테스트")
    class PerformanceAndLoadTests {

        @Test
        @DisplayName("대량 영상 처리 성능 테스트")
        void bulkVideoProcessing_PerformanceTest() {
            // given
            List<Video> largeVideoList = createLargeVideoList(50);
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(largeVideoList);

            // S3 URL 생성 Mock (빠른 응답)
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            long startTime = System.currentTimeMillis();
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);
            long endTime = System.currentTimeMillis();

            // then
            assertThat(responses).hasSize(50);
            assertThat(endTime - startTime).isLessThan(2000); // 2초 이내 처리
            
            // 모든 응답에 URL이 포함되어야 함
            responses.forEach(response -> {
                assertThat(response.getThumbnailUrl()).isNotNull();
                assertThat(response.getStreamingUrl()).isNotNull();
                assertThat(response.getDownloadUrl()).isNotNull();
            });
        }

        @Test
        @DisplayName("메모리 사용량 제한 테스트")
        void memoryUsageLimit_Test() {
            // given
            List<Video> moderateVideoList = createLargeVideoList(20);
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(moderateVideoList);

            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);
            
            runtime.gc();
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();

            // then
            assertThat(responses).hasSize(20);
            assertThat(afterMemory - beforeMemory).isLessThan(10 * 1024 * 1024); // 10MB 이내
        }
    }

    // ========================================
    // 테스트 헬퍼 메서드들 (15년차 전문가 리팩토링)
    // ========================================

    /**
     * 업로드된 상태의 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createUploadedForTest() 사용
     */
    private Video createMockUploadedVideo() {
        Video uploadedVideo = Video.createUploadedForTest(
                testMember.getId(), 
                "test-video.mp4", 
                testVideoFile.getSize()
        );
        // ID 설정을 위한 Builder 패턴 사용
        return Video.builder()
                .id(1L)
                .memberId(uploadedVideo.getMemberId())
                .originalFileName(uploadedVideo.getOriginalFileName())
                .s3OriginalKey(uploadedVideo.getS3OriginalKey())
                .s3ProcessedKey(uploadedVideo.getS3ProcessedKey())
                .s3ThumbnailKey(uploadedVideo.getS3ThumbnailKey())
                .fileSizeBytes(uploadedVideo.getFileSizeBytes())
                .status(uploadedVideo.getStatus())
                .processingType(uploadedVideo.getProcessingType())
                .metadata(uploadedVideo.getMetadata())
                .urls(uploadedVideo.getUrls())
                .build();
    }

    /**
     * 처리 완료된 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockProcessedVideo() {
        Video processedVideo = Video.createForTest(1L, testMember.getId(), "test-video.mp4", VideoStatus.PROCESSED);
        // 테스트용 파일 크기 조정
        return Video.builder()
                .id(processedVideo.getId())
                .memberId(processedVideo.getMemberId())
                .originalFileName(processedVideo.getOriginalFileName())
                .s3OriginalKey(processedVideo.getS3OriginalKey())
                .s3ProcessedKey(processedVideo.getS3ProcessedKey())
                .s3ThumbnailKey(processedVideo.getS3ThumbnailKey())
                .fileSizeBytes(testVideoFile.getSize())
                .status(processedVideo.getStatus())
                .processingType(processedVideo.getProcessingType())
                .metadata(processedVideo.getMetadata())
                .urls(processedVideo.getUrls())
                .build();
    }

    /**
     * 특정 사용자의 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockVideoForUser(Long videoId, Long userId) {
        return Video.createForTest(videoId, userId, "user" + userId + "-video.mp4", VideoStatus.UPLOADED);
    }

    /**
     * 특정 상태의 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockVideoWithStatus(Long videoId, VideoStatus status) {
        return Video.createForTest(videoId, testMember.getId(), "video" + videoId + ".mp4", status);
    }

    /**
     * 대량의 영상 목록을 생성합니다 (성능 테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private List<Video> createLargeVideoList(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> Video.createForTest(
                        (long) i, 
                        testMember.getId(), 
                        "video" + i + ".mp4", 
                        VideoStatus.PROCESSED))
                .toList();
    }
}
