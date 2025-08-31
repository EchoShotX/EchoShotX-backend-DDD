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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * GetProcessedVideosUseCase 테스트
 * 
 * ⚠️  주요 변경사항: 썸네일 URL만 제공하고 스트리밍 기능은 보류
 * - 썸네일 URL 생성 및 검증 테스트 활성화
 * - 스트리밍 관련 테스트는 @Disabled로 보류 처리
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
        @DisplayName("처리된 영상 목록 조회 - 썸네일 URL 포함")
        void getProcessedVideos_WithThumbnailUrl_Success() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            
            // 디버깅: 실제 생성된 Video 객체 상태 확인
            System.out.println("Created mock videos:");
            for (Video video : mockVideos) {
                System.out.println("Video ID: " + video.getId() + 
                    ", Status: " + video.getStatus() + 
                    ", ThumbnailKey: " + video.getS3ThumbnailKey());
            }
            
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            // S3 썸네일 URL 생성 Mock 설정
            given(awsS3Service.generateFileUrl("thumbnails/thumb_1.jpg"))
                    .willReturn("https://echoshotx-bucket.s3.ap-northeast-2.amazonaws.com/thumbnails/thumb_1.jpg");
            given(awsS3Service.generateFileUrl("thumbnails/thumb_2.jpg"))
                    .willReturn("https://echoshotx-bucket.s3.ap-northeast-2.amazonaws.com/thumbnails/thumb_2.jpg");

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            
            VideoListResponse response1 = responses.get(0);
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response1.getStatus()).isEqualTo(VideoStatus.PROCESSED);
            assertThat(response1.getOriginalFileName()).isEqualTo("video1.mp4");
            assertThat(response1.getFileSizeBytes()).isEqualTo(1024L);
            assertThat(response1.getProcessingType()).isEqualTo(ProcessingType.BASIC_ENHANCEMENT);
            assertThat(response1.getS3ThumbnailKey()).isEqualTo("thumbnails/thumb_1.jpg");
            assertThat(response1.getThumbnailUrl()).contains("thumbnails/thumb_1.jpg");
            assertThat(response1.getUploadedAt()).isNotNull();
            assertThat(response1.getUpdatedAt()).isNotNull();

            VideoListResponse response2 = responses.get(1);
            assertThat(response2.getVideoId()).isEqualTo(2L);
            assertThat(response2.getThumbnailUrl()).contains("thumbnails/thumb_2.jpg");

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            verify(awsS3Service).generateFileUrl("thumbnails/thumb_1.jpg");
            verify(awsS3Service).generateFileUrl("thumbnails/thumb_2.jpg");
        }

        @Test
        @DisplayName("Video.createForTest() 동작 확인")
        void videoCreateForTest_Behavior_Verification() {
            // given
            Video testVideo = Video.createForTest(1L, testMember.getId(), "test.mp4", VideoStatus.PROCESSED);
            
            // then
            System.out.println("Test Video - ID: " + testVideo.getId() + 
                ", Status: " + testVideo.getStatus() + 
                ", ThumbnailKey: " + testVideo.getS3ThumbnailKey());
            
            assertThat(testVideo.getId()).isEqualTo(1L);
            assertThat(testVideo.getStatus()).isEqualTo(VideoStatus.PROCESSED);
            assertThat(testVideo.getS3ThumbnailKey()).isEqualTo("thumbnails/thumb_1.jpg");
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
        @DisplayName("썸네일 키가 없는 영상 - 기본 정보만 반환")
        void getProcessedVideos_NoThumbnailKey_Success() {
            // given
            Video videoWithoutThumbnail = Video.builder()
                    .id(1L)
                    .memberId(testMember.getId())
                    .originalFileName("video_no_thumb.mp4")
                    .s3OriginalKey("originals/video_no_thumb.mp4")
                    .s3ProcessedKey("processed/video_no_thumb.mp4")
                    .s3ThumbnailKey(null) // 썸네일 키 없음
                    .fileSizeBytes(2048L)
                    .status(VideoStatus.PROCESSED)
                    .processingType(ProcessingType.BASIC_ENHANCEMENT)
                    .metadata(VideoMetadata.createEmptyForTest())
                    .urls(VideoUrls.empty())
                    .build();

            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(videoWithoutThumbnail));

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(1);
            
            VideoListResponse response = responses.get(0);
            assertThat(response.getVideoId()).isEqualTo(1L);
            assertThat(response.getS3ThumbnailKey()).isNull();
            assertThat(response.getThumbnailUrl()).isNull(); // 썸네일 URL도 null
            assertThat(response.getOriginalFileName()).isEqualTo("video_no_thumb.mp4");

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            // generateFileUrl이 호출되지 않아야 함
            verify(awsS3Service, never()).generateFileUrl(anyString());
        }

        @Test
        @DisplayName("썸네일 URL 생성 실패 - Graceful Degradation")
        void getProcessedVideos_ThumbnailUrlGenerationFails_GracefulDegradation() {
            // given
            List<Video> mockVideos = createMockProcessedVideos();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(mockVideos);

            // S3 URL 생성 시 예외 발생
            given(awsS3Service.generateFileUrl(anyString()))
                    .willThrow(new RuntimeException("S3 connection failed"));

            // when
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);

            // then
            assertThat(responses).hasSize(2);
            
            VideoListResponse response1 = responses.get(0);
            assertThat(response1.getVideoId()).isEqualTo(1L);
            assertThat(response1.getThumbnailUrl()).isNull(); // URL 생성 실패 시 null
            assertThat(response1.getOriginalFileName()).isEqualTo("video1.mp4"); // 기본 정보는 제공

            verify(videoAdaptor).queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED);
            verify(awsS3Service, times(2)).generateFileUrl(anyString());
        }

        @Test
        @DisplayName("영상 메타데이터 정확성 검증")
        void videoMetadata_AccuracyValidation() {
            // given
            Video video = createDetailedMockVideo();
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(List.of(video));

            // S3 썸네일 URL 생성 Mock 설정
            given(awsS3Service.generateFileUrl("thumbnails/thumb_1.jpg"))
                    .willReturn("https://echoshotx-bucket.s3.ap-northeast-2.amazonaws.com/thumbnails/thumb_1.jpg");

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
            assertThat(response.getThumbnailUrl()).contains("thumbnails/thumb_1.jpg");
            assertThat(response.getUploadedAt()).isEqualTo(video.getCreatedDate());
        }
    }

    // ========================================
    // 스트리밍 관련 테스트 (향후 구현 예정으로 보류)
    // ========================================
    
    @Nested
    @DisplayName("스트리밍 기능 테스트 (보류)")
    class StreamingFeatureTests {

        @Test
        @Disabled("스트리밍 기능이 향후 구현 예정으로 보류")
        @DisplayName("스트리밍 URL 생성 성공")
        void generateStreamingUrl_Success() {
            // TODO: 향후 스트리밍 기능 구현 시 활성화
            // - generateStreamingUrl() 테스트
            // - generateDownloadUrl() 테스트
            // - urlExpiresAt 검증
        }

        @Test
        @Disabled("스트리밍 기능이 향후 구현 예정으로 보류")
        @DisplayName("스트리밍 URL 생성 실패")
        void generateStreamingUrl_Failure() {
            // TODO: 향후 스트리밍 기능 구현 시 활성화
        }
    }

    @Nested
    @DisplayName("성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("대량 영상 목록 처리 - 썸네일만")
        void largeVideoList_ThumbnailOnly_PerformanceTest() {
            // given
            List<Video> largeVideoList = createLargeVideoList(50);
            given(videoAdaptor.queryAllByMemberIdAndStatus(testMember.getId(), VideoStatus.PROCESSED))
                    .willReturn(largeVideoList);

            // S3 URL 생성 성공으로 설정
            given(awsS3Service.generateFileUrl(anyString()))
                    .willReturn("https://echoshotx-bucket.s3.ap-northeast-2.amazonaws.com/thumbnail.jpg");

            // when
            long startTime = System.currentTimeMillis();
            List<VideoListResponse> responses = getProcessedVideosUseCase.execute(testMember);
            long endTime = System.currentTimeMillis();

            // then
            assertThat(responses).hasSize(50);
            assertThat(endTime - startTime).isLessThan(2000); // 2초 이내 처리 (썸네일만이므로 더 빠름)

            verify(awsS3Service, times(50)).generateFileUrl(anyString());
        }
    }

    // ========================================
    // 테스트 헬퍼 메서드들
    // ========================================

    /**
     * 처리된 영상 목록을 생성합니다 (테스트용)
     */
    private List<Video> createMockProcessedVideos() {
        return Arrays.asList(
                Video.createForTest(1L, testMember.getId(), "video1.mp4", VideoStatus.PROCESSED),
                Video.createForTest(2L, testMember.getId(), "video2.mp4", VideoStatus.PROCESSED)
        );
    }



    /**
     * 상세 메타데이터가 포함된 영상을 생성합니다 (테스트용)
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