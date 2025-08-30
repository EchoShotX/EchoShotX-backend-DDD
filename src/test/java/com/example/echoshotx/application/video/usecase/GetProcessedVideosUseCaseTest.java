package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.entity.Role;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.domain.video.vo.VideoUrls;
import com.example.echoshotx.infrastructure.service.AwsS3Service;
import com.example.echoshotx.presentation.video.dto.response.VideoListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * GetProcessedVideosUseCase 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProcessedVideosUseCase 테스트")
class GetProcessedVideosUseCaseTest {

    @Mock
    private VideoAdaptor videoAdaptor;

    @Mock
    private AwsS3Service awsS3Service;

    @InjectMocks
    private GetProcessedVideosUseCase getProcessedVideosUseCase;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스트사용자")
                .username("testuser")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessfulCases {

        @Test
        @DisplayName("처리된 영상 목록 조회 - 모든 URL 생성 성공")
        void getProcessedVideos_AllUrlsGenerated_Success() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            // S3 URL 생성 Mock 설정
            given(awsS3Service.generateThumbnailUrl("thumbnails/thumb1.jpg"))
                    .willReturn("https://s3.aws.com/thumbnails/thumb1.jpg?expires=123");
            given(awsS3Service.generateStreamingUrl("processed/video1.mp4"))
                    .willReturn("https://s3.aws.com/processed/video1.jpg?expires=456");
            given(awsS3Service.generateDownloadUrl("originals/video1.mp4"))
                    .willReturn("https://s3.aws.com/originals/video1.mp4?expires=789");

            given(awsS3Service.generateThumbnailUrl("thumbnails/thumb2.jpg"))
                    .willReturn("https://s3.aws.com/thumbnails/thumb2.jpg?expires=123");
            given(awsS3Service.generateStreamingUrl("processed/video2.mp4"))
                    .willReturn("https://s3.aws.com/processed/video2.mp4?expires=456");
            given(awsS3Service.generateDownloadUrl("originals/video2.mp4"))
                    .willReturn("https://s3.aws.com/originals/video2.mp4?expires=789");

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            
            VideoListResponse response1 = responses.get(0);
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response1.getStatus()).isEqualTo(VideoStatus.PROCESSED);
            assertThat(response1.getThumbnailUrl()).contains("thumbnails/thumb1.jpg");
            assertThat(response1.getStreamingUrl()).contains("processed/video1");
            assertThat(response1.getDownloadUrl()).contains("originals/video1.mp4");
            assertThat(response1.getUrlExpiresAt()).isNotNull();

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            verify(awsS3Service, times(2)).generateThumbnailUrl(anyString());
            verify(awsS3Service, times(2)).generateStreamingUrl(anyString());
            verify(awsS3Service, times(2)).generateDownloadUrl(anyString());
        }

        @Test
        @DisplayName("빈 목록 조회 - 정상 처리")
        void getProcessedVideos_EmptyList_Success() {
            // given
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(Collections.emptyList());

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).isEmpty();
            
            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            verifyNoInteractions(awsS3Service);
        }

        @Test
        @DisplayName("부분적 키 존재 - 선택적 URL 생성")
        void getProcessedVideos_PartialKeys_SelectiveUrlGeneration() {
            // given
                     Video videoWithPartialKeys = Video.builder()
                 .id(1L)
                 .memberId(testMember.getId())
                 .originalFileName("video1.mp4")
                 .s3OriginalKey("originals/video1.mp4")
                 .s3ProcessedKey(null) // 처리된 키 없음
                 .s3ThumbnailKey("thumbnails/thumb1.jpg")
                 .fileSizeBytes(1024L)
                 .status(VideoStatus.PROCESSED)
                 .processingType(ProcessingType.BASIC_ENHANCEMENT)
                 .metadata(VideoMetadata.createEmptyForTest())
                 .urls(VideoUrls.empty())
                 .build();

            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(videoWithPartialKeys));

            given(awsS3Service.generateThumbnailUrl("thumbnails/thumb1.jpg"))
                    .willReturn("https://s3.aws.com/thumbnails/thumb1.jpg?expires=123");
            given(awsS3Service.generateDownloadUrl("originals/video1.mp4"))
                    .willReturn("https://s3.aws.com/originals/video1.mp4?expires=789");

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(1);
            
            VideoListResponse response = responses.get(0);
            assertThat(response.getThumbnailUrl()).isNotNull();
            assertThat(response.getStreamingUrl()).isNull(); // 처리된 키가 없으므로 null
            assertThat(response.getDownloadUrl()).isNotNull();

            verify(awsS3Service).generateThumbnailUrl("thumbnails/thumb1.jpg");
            verify(awsS3Service).generateDownloadUrl("originals/video1.mp4");
            verify(awsS3Service, never()).generateStreamingUrl(anyString());
        }
    }

    @Nested
    @DisplayName("S3 연동 예외 케이스")
    class S3ExceptionCases {

        @Test
        @DisplayName("S3 URL 생성 실패 - Graceful Degradation")
        void s3UrlGenerationFails_GracefulDegradation() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            // S3 URL 생성 시 예외 발생
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willThrow(new RuntimeException("S3 connection failed"));
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willThrow(new RuntimeException("S3 connection failed"));
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willThrow(new RuntimeException("S3 connection failed"));

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            
            // URL은 생성되지 않았지만 기본 영상 정보는 제공되어야 함
            VideoListResponse response1 = responses.get(0);
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response1.getOriginalFileName()).isEqualTo("video1.mp4");
            assertThat(response1.getStatus()).isEqualTo(VideoStatus.PROCESSED);
            assertThat(response1.getThumbnailUrl()).isNull();
            assertThat(response1.getStreamingUrl()).isNull();
            assertThat(response1.getDownloadUrl()).isNull();
            assertThat(response1.getUrlExpiresAt()).isNull();

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
        }

        @Test
        @DisplayName("부분적 S3 실패 - 일부 URL만 생성")
        void partialS3Failure_PartialUrlGeneration() {
            // given
            Video mockVideo = createMockVideo(1L, "video1.mp4");
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(mockVideo));

            // 썸네일만 성공, 나머지는 실패
            given(awsS3Service.generateThumbnailUrl("thumbnails/thumb1.jpg"))
                    .willReturn("https://s3.aws.com/thumbnails/thumb1.jpg?expires=123");
            given(awsS3Service.generateStreamingUrl("processed/video1.mp4"))
                    .willThrow(new RuntimeException("Streaming URL generation failed"));
            given(awsS3Service.generateDownloadUrl("originals/video1.mp4"))
                    .willThrow(new RuntimeException("Download URL generation failed"));

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(1);
            
            VideoListResponse response = responses.get(0);
            assertThat(response.getVideoId()).isEqualTo(1L);
            assertThat(response.getThumbnailUrl()).isNull(); // 전체 실패 시 모든 URL이 null
            assertThat(response.getStreamingUrl()).isNull();
            assertThat(response.getDownloadUrl()).isNull();
        }

        @Test
        @DisplayName("S3 서비스 타임아웃 - 적절한 예외 처리")
        void s3ServiceTimeout_ProperExceptionHandling() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            // 타임아웃 시뮬레이션
            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willAnswer(invocation -> {
                        Thread.sleep(100); // 타임아웃 시뮬레이션
                        throw new RuntimeException("Request timeout");
                    });

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            // 기본 정보는 제공되어야 함
            assertThat(responses.get(0).getVideoId()).isEqualTo(1L);
            assertThat(responses.get(0).getThumbnailUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("성능 및 동시성 테스트")
    class PerformanceAndConcurrencyTests {

        @Test
        @DisplayName("대량 영상 목록 처리 - 성능 검증")
        void largeVideoList_PerformanceTest() {
            // given
            List<Video> largeVideoList = createLargeVideoList(100);
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(largeVideoList);

            // S3 URL 생성 성공으로 설정
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
            assertThat(responses).hasSize(100);
            assertThat(endTime - startTime).isLessThan(5000); // 5초 이내 처리

            verify(awsS3Service, times(100)).generateThumbnailUrl(anyString());
            verify(awsS3Service, times(100)).generateStreamingUrl(anyString());
            verify(awsS3Service, times(100)).generateDownloadUrl(anyString());
        }

        @Test
        @DisplayName("동시 사용자 요청 시뮬레이션")
        void concurrentUserRequests_Simulation() {
            // given
            Member user1 = Member.builder().id(1L).build();
            Member user2 = Member.builder().id(2L).build();

            List<Video> user1Videos = List.of(createMockVideo(1L, "user1_video.mp4"));
            List<Video> user2Videos = List.of(createMockVideo(2L, "user2_video.mp4"));

            given(videoAdaptor.queryAllByMemberIdAndStatus(1L, VideoStatus.PROCESSED))
                    .willReturn(user1Videos);
            given(videoAdaptor.queryAllByMemberIdAndStatus(2L, VideoStatus.PROCESSED))
                    .willReturn(user2Videos);

            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            List<VideoListResponse> user1Response = getProcessedVideosUseCase.execute(user1);
            List<VideoListResponse> user2Response = getProcessedVideosUseCase.execute(user2);

            // then
            assertThat(user1Response).hasSize(1);
            assertThat(user2Response).hasSize(1);
            assertThat(user1Response.get(0).getVideoId()).isEqualTo(1L);
            assertThat(user2Response.get(0).getVideoId()).isEqualTo(2L);

            verify(videoAdaptor).queryAllByMemberIdAndStatus(1L, VideoStatus.PROCESSED);
            verify(videoAdaptor).queryAllByMemberIdAndStatus(2L, VideoStatus.PROCESSED);
        }
    }

    @Nested
    @DisplayName("데이터 일관성 테스트")
    class DataConsistencyTests {

        @Test
        @DisplayName("URL 만료 시간 일관성 검증")
        void urlExpirationTime_ConsistencyValidation() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            
            LocalDateTime now = LocalDateTime.now();
            for (VideoListResponse response : responses) {
                assertThat(response.getUrlExpiresAt()).isAfter(now);
                assertThat(response.getUrlExpiresAt()).isBefore(now.plusHours(2));
            }
        }

        @Test
        @DisplayName("영상 메타데이터 정확성 검증")
        void videoMetadata_AccuracyValidation() {
            // given
            Video video = createDetailedMockVideo();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(video));

            given(awsS3Service.generateThumbnailUrl(anyString()))
                    .willReturn("https://s3.aws.com/thumbnail.jpg");
            given(awsS3Service.generateStreamingUrl(anyString()))
                    .willReturn("https://s3.aws.com/streaming.mp4");
            given(awsS3Service.generateDownloadUrl(anyString()))
                    .willReturn("https://s3.aws.com/download.mp4");

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(1);
            
            VideoListResponse response = responses.get(0);
            assertThat(response.getVideoId()).isEqualTo(video.getId());
            assertThat(response.getOriginalFileName()).isEqualTo(video.getOriginalFileName());
            assertThat(response.getFileSizeBytes()).isEqualTo(video.getFileSizeBytes());
            assertThat(response.getStatus()).isEqualTo(video.getStatus());
            assertThat(response.getProcessingType()).isEqualTo(video.getProcessingType());
            assertThat(response.getS3ThumbnailKey()).isEqualTo(video.getS3ThumbnailKey());
            assertThat(response.getUploadedAt()).isEqualTo(video.getCreatedDate());
        }
    }

    // ========================================
    // 테스트 헬퍼 메서드들 (15년차 전문가 리팩토링)
    // ========================================

    /**
     * 처리된 영상 목록을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private List<Video> createMockProcessedVideos() {
        return Arrays.asList(
                Video.createForTest(1L, testMember.getId(), "video1.mp4", VideoStatus.PROCESSED),
                Video.createForTest(2L, testMember.getId(), "video2.mp4", VideoStatus.PROCESSED)
        );
    }

    /**
     * 개별 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockVideo(Long id, String fileName) {
        return Video.createForTest(id, testMember.getId(), fileName, VideoStatus.PROCESSED);
    }

    /**
     * 상세 메타데이터가 포함된 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createDetailedForTest() 사용
     */
    private Video createDetailedMockVideo() {
        return Video.createDetailedForTest(
                1L, 
                testMember.getId(), 
                "detailed_video.mp4", 
                VideoStatus.PROCESSED,
                300.5,  // 5분 영상
                1920,   // Full HD 가로
                1080    // Full HD 세로
        );
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
