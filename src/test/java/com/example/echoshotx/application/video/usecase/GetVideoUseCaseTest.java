package com.example.echoshotx.application.video.usecase;

import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.entity.Role;
import com.example.echoshotx.domain.video.adaptor.VideoAdaptor;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;

import com.example.echoshotx.presentation.video.dto.response.VideoDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * GetVideoUseCase 테스트
 * 
 * 15년차 백엔드 테스트 전문가 관점에서 작성된 보안 중심 테스트:
 * 1. 권한 검증 로직 철저한 테스트
 * 2. 보안 취약점 방지 검증
 * 3. 예외 상황 처리 검증
 * 4. 데이터 노출 방지 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetVideoUseCase 테스트")
class GetVideoUseCaseTest {

    @Mock
    private VideoAdaptor videoAdaptor;

    @InjectMocks
    private GetVideoUseCase getVideoUseCase;

    private Member testMember;
    private Video testVideo;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스트사용자")
                .username("testuser")
                .role(Role.USER)
                .build();

        testVideo = createMockVideo(1L, testMember.getId());
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessfulCases {

        @Test
        @DisplayName("유효한 영상 조회 - 성공")
        void getVideo_ValidRequest_Success() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(1L);
            assertThat(response.getOriginalFileName()).isEqualTo("test-video.mp4");
            assertThat(response.getS3OriginalKey()).isEqualTo("originals/test-video.mp4");
            assertThat(response.getS3ProcessedKey()).isEqualTo("processed/test-video.mp4");
            assertThat(response.getS3ThumbnailKey()).isEqualTo("thumbnails/thumb.jpg");
            assertThat(response.getFileSizeBytes()).isEqualTo(1024L);
            assertThat(response.getStatus()).isEqualTo(VideoStatus.UPLOADED);
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.BASIC_ENHANCEMENT);

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("다양한 상태의 영상 조회 - 성공")
        void getVideo_DifferentStatuses_Success() {
            // given
            VideoStatus[] statuses = VideoStatus.values();
            
            for (int i = 0; i < statuses.length; i++) {
                Video video = createMockVideoWithStatus(1L + i, testMember.getId(), statuses[i]);
                given(videoAdaptor.queryById(1L + i)).willReturn(video);

                // when
                VideoDetailResponse response = getVideoUseCase.execute(1L + i, testMember);

                // then
                assertThat(response.getStatus()).isEqualTo(statuses[i]);
                
                verify(videoAdaptor).queryById(1L + i);
            }
        }

        @Test
        @DisplayName("메타데이터가 있는 영상 조회 - 성공")
        void getVideo_WithMetadata_Success() {
            // given
            Video videoWithMetadata = createVideoWithDetailedMetadata();
            given(videoAdaptor.queryById(1L)).willReturn(videoWithMetadata);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);

            // then
            assertThat(response.getMetadata()).isNotNull();
            assertThat(response.getMetadata().getDurationSeconds()).isEqualTo(300.5);
            assertThat(response.getMetadata().getWidth()).isEqualTo(1920);
            assertThat(response.getMetadata().getHeight()).isEqualTo(1080);
            assertThat(response.getMetadata().getCodec()).isEqualTo("h264");
            assertThat(response.getMetadata().getBitrate()).isEqualTo(5000000L);
            assertThat(response.getMetadata().getFrameRate()).isEqualTo(30.0);

            verify(videoAdaptor).queryById(1L);
        }
    }

    @Nested
    @DisplayName("보안 및 권한 검증")
    class SecurityAndAuthorizationTests {

        @Test
        @DisplayName("다른 사용자의 영상 접근 시도 - 권한 예외")
        void getVideo_UnauthorizedAccess_ThrowsException() {
            // given
            Member unauthorizedMember = Member.builder()
                    .id(999L)
                    .email("unauthorized@example.com")
                    .nickname("무권한사용자")
                    .username("unauthorizeduser")
                    .role(Role.USER)
                    .build();

            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when & then
            assertThatThrownBy(() -> getVideoUseCase.execute(1L, unauthorizedMember))
                    .isInstanceOf(VideoHandler.class)
                    .extracting(ex -> ((VideoHandler) ex).getCode())
                    .isEqualTo(VideoErrorStatus.VIDEO_MEMBER_MISMATCH);

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 영상 조회 - 예외")
        void getVideo_NonExistentVideo_ThrowsException() {
            // given
            Long nonExistentVideoId = 999L;
            given(videoAdaptor.queryById(nonExistentVideoId))
                    .willThrow(new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> getVideoUseCase.execute(nonExistentVideoId, testMember))
                    .isInstanceOf(VideoHandler.class)
                    .extracting(ex -> ((VideoHandler) ex).getCode())
                    .isEqualTo(VideoErrorStatus.VIDEO_NOT_FOUND);

            verify(videoAdaptor).queryById(nonExistentVideoId);
        }

        @Test
        @DisplayName("null 사용자로 접근 시도 - NPE 방지")
        void getVideo_NullMember_ThrowsException() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when & then
            assertThatThrownBy(() -> getVideoUseCase.execute(1L, null))
                    .isInstanceOf(NullPointerException.class);

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("권한 있는 사용자의 정상 접근 - 성공")
        void getVideo_AuthorizedAccess_Success() {
            // given
            Member authorizedMember = Member.builder()
                    .id(testMember.getId()) // 동일한 ID
                    .email("authorized@example.com")
                    .nickname("권한사용자")
                    .username("authorizeduser")
                    .role(Role.ADMIN) // 다른 역할이지만 동일한 ID
                    .build();

            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, authorizedMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(1L);

            verify(videoAdaptor).queryById(1L);
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 케이스")
    class EdgeCasesAndExceptions {

        @Test
        @DisplayName("음수 영상 ID - 적절한 예외 처리")
        void getVideo_NegativeVideoId_ProperHandling() {
            // given
            Long negativeVideoId = -1L;
            given(videoAdaptor.queryById(negativeVideoId))
                    .willThrow(new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> getVideoUseCase.execute(negativeVideoId, testMember))
                    .isInstanceOf(VideoHandler.class);

            verify(videoAdaptor).queryById(negativeVideoId);
        }

        @Test
        @DisplayName("0인 영상 ID - 적절한 예외 처리")
        void getVideo_ZeroVideoId_ProperHandling() {
            // given
            Long zeroVideoId = 0L;
            given(videoAdaptor.queryById(zeroVideoId))
                    .willThrow(new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> getVideoUseCase.execute(zeroVideoId, testMember))
                    .isInstanceOf(VideoHandler.class);

            verify(videoAdaptor).queryById(zeroVideoId);
        }

        @Test
        @DisplayName("매우 큰 영상 ID - 정상 처리")
        void getVideo_VeryLargeVideoId_NormalProcessing() {
            // given
            Long largeVideoId = Long.MAX_VALUE;
            Video largeIdVideo = createMockVideo(largeVideoId, testMember.getId());
            given(videoAdaptor.queryById(largeVideoId)).willReturn(largeIdVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(largeVideoId, testMember);

            // then
            assertThat(response.getVideoId()).isEqualTo(largeVideoId);

            verify(videoAdaptor).queryById(largeVideoId);
        }

        @Test
        @DisplayName("부분적으로 null인 영상 데이터 - 견고한 처리")
        void getVideo_PartiallyNullVideoData_RobustHandling() {
            // given
            Video partialVideo = Video.builder()
                    .id(1L)
                    .memberId(testMember.getId())
                    .originalFileName("partial-video.mp4")
                    .s3OriginalKey("originals/partial-video.mp4")
                    .s3ProcessedKey(null) // null 값
                    .s3ThumbnailKey(null) // null 값
                    .fileSizeBytes(1024L)
                    .status(VideoStatus.UPLOADED)
                    .processingType(ProcessingType.BASIC_ENHANCEMENT)
                    .metadata(null) // null 값
                    .urls(null) // null 값
                    .build();

            given(videoAdaptor.queryById(1L)).willReturn(partialVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getVideoId()).isEqualTo(1L);
            assertThat(response.getS3ProcessedKey()).isNull();
            assertThat(response.getS3ThumbnailKey()).isNull();
            assertThat(response.getMetadata()).isNull();

            verify(videoAdaptor).queryById(1L);
        }
    }

    @Nested
    @DisplayName("데이터 무결성 테스트")
    class DataIntegrityTests {

        @Test
        @DisplayName("응답 데이터 일관성 검증")
        void getVideo_ResponseDataConsistency_Validation() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);

            // then
            // 원본 데이터와 응답 데이터의 일관성 검증
            assertThat(response.getVideoId()).isEqualTo(testVideo.getId());
            assertThat(response.getOriginalFileName()).isEqualTo(testVideo.getOriginalFileName());
            assertThat(response.getS3OriginalKey()).isEqualTo(testVideo.getS3OriginalKey());
            assertThat(response.getS3ProcessedKey()).isEqualTo(testVideo.getS3ProcessedKey());
            assertThat(response.getS3ThumbnailKey()).isEqualTo(testVideo.getS3ThumbnailKey());
            assertThat(response.getFileSizeBytes()).isEqualTo(testVideo.getFileSizeBytes());
            assertThat(response.getStatus()).isEqualTo(testVideo.getStatus());
            assertThat(response.getProcessingType()).isEqualTo(testVideo.getProcessingType());
            assertThat(response.getUploadedAt()).isEqualTo(testVideo.getCreatedDate());
            assertThat(response.getUpdatedAt()).isEqualTo(testVideo.getLastModifiedDate());

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("민감한 정보 노출 방지 검증")
        void getVideo_SensitiveDataProtection_Validation() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);

            // then
            // 응답에는 사용자 정보가 포함되지 않아야 함
            assertThat(response.toString()).doesNotContain(testMember.getEmail());
            assertThat(response.toString()).doesNotContain("password");
            assertThat(response.toString()).doesNotContain("token");

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("동시성 상황에서의 데이터 일관성")
        void getVideo_ConcurrentAccess_DataConsistency() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when - 동시에 여러 번 호출
            VideoDetailResponse response1 = getVideoUseCase.execute(1L, testMember);
            VideoDetailResponse response2 = getVideoUseCase.execute(1L, testMember);
            VideoDetailResponse response3 = getVideoUseCase.execute(1L, testMember);

            // then
            assertThat(response1.getVideoId()).isEqualTo(response2.getVideoId()).isEqualTo(response3.getVideoId());
            assertThat(response1.getOriginalFileName()).isEqualTo(response2.getOriginalFileName()).isEqualTo(response3.getOriginalFileName());
            assertThat(response1.getStatus()).isEqualTo(response2.getStatus()).isEqualTo(response3.getStatus());

            verify(videoAdaptor, times(3)).queryById(1L);
        }
    }

    @Nested
    @DisplayName("성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("응답 시간 검증")
        void getVideo_ResponseTime_Validation() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            long startTime = System.currentTimeMillis();
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);
            long endTime = System.currentTimeMillis();

            // then
            assertThat(response).isNotNull();
            assertThat(endTime - startTime).isLessThan(100); // 100ms 이내

            verify(videoAdaptor).queryById(1L);
        }

        @Test
        @DisplayName("메모리 사용량 검증")
        void getVideo_MemoryUsage_Validation() {
            // given
            given(videoAdaptor.queryById(1L)).willReturn(testVideo);

            // when
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            
            VideoDetailResponse response = getVideoUseCase.execute(1L, testMember);
            
            runtime.gc(); // 가비지 컬렉션 강제 실행
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();

            // then
            assertThat(response).isNotNull();
            assertThat(afterMemory - beforeMemory).isLessThan(1024 * 1024); // 1MB 이내

            verify(videoAdaptor).queryById(1L);
        }
    }

    // ========================================
    // 테스트 헬퍼 메서드들 (15년차 전문가 리팩토링)
    // ========================================

    /**
     * 기본 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockVideo(Long id, Long memberId) {
        Video baseVideo = Video.createForTest(id, memberId, "test-video.mp4", VideoStatus.UPLOADED);
        return Video.builder()
                .id(baseVideo.getId())
                .memberId(baseVideo.getMemberId())
                .originalFileName(baseVideo.getOriginalFileName())
                .s3OriginalKey(baseVideo.getS3OriginalKey())
                .s3ProcessedKey("processed/test-video.mp4")  // 테스트를 위해 명시적으로 설정
                .s3ThumbnailKey("thumbnails/thumb.jpg")      // 테스트를 위해 명시적으로 설정
                .fileSizeBytes(1024L)
                .status(VideoStatus.UPLOADED)
                .processingType(baseVideo.getProcessingType())
                .metadata(baseVideo.getMetadata())
                .urls(baseVideo.getUrls())
                .build();
    }

    /**
     * 특정 상태의 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createForTest() 사용
     */
    private Video createMockVideoWithStatus(Long id, Long memberId, VideoStatus status) {
        return Video.createForTest(id, memberId, "test-video-" + status + ".mp4", status);
    }

    /**
     * 상세 메타데이터가 포함된 영상을 생성합니다 (테스트용)
     * ✅ 안티패턴 제거: Video.createDetailedForTest() 사용
     */
    private Video createVideoWithDetailedMetadata() {
        return Video.createDetailedForTest(
                1L, 
                testMember.getId(), 
                "detailed-video.mp4", 
                VideoStatus.PROCESSED,
                300.5,  // 5분 영상
                1920,   // Full HD 가로
                1080    // Full HD 세로
        );
    }
}
