package com.example.echoshotx.domain.credit.service;

import com.example.echoshotx.domain.credit.entity.CreditHistory;
import com.example.echoshotx.domain.credit.exception.CreditHandler;
import com.example.echoshotx.domain.credit.repository.CreditHistoryRepository;
import com.example.echoshotx.domain.member.adaptor.MemberAdaptor;
import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.entity.Role;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditService 단위 테스트")
class CreditServiceTest {

    @Mock
    private MemberAdaptor memberAdaptor;
    
    @Mock
    private CreditHistoryRepository creditHistoryRepository;
    
    @InjectMocks
    private CreditService creditService;
    
    private Member testMember;
    private Video testVideo;
    
    @BeforeEach
    void setUp() {
        // 테스트용 Member 생성
        testMember = Member.builder()
                .id(1L)
                .username("testuser")
                .nickname("테스트유저")
                .email("test@test.com")
                .role(Role.USER)
                .currentCredits(1000)  // 충분한 크레딧
                .build();
        
        // 테스트용 Video 생성
        VideoMetadata metadata = new VideoMetadata(300.0, 1920, 1080, "h264", 5000000L, 30.0);
        testVideo = Video.builder()
                .id(1L)
                .memberId(1L)
                .originalFileName("test-video.mp4")
                .s3OriginalKey("videos/test-video.mp4")
                .fileSizeBytes(100000000L)
                .status(VideoStatus.UPLOADED)
                .metadata(metadata)
                .build();
    }
    
    @Nested
    @DisplayName("크레딧 사용 테스트")
    class UseCreditsTest {
        
        @Test
        @DisplayName("성공: 충분한 크레딧으로 영상 처리")
        void useCreditsForVideoProcessing_Success() {
            // given
            ProcessingType processingType = ProcessingType.BASIC_ENHANCEMENT;
            CreditHistory expectedHistory = CreditHistory.createUsage(1L, 1L, 300, processingType);
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(expectedHistory);
            
            // when
            CreditHistory result = creditService.useCreditsForVideoProcessing(testVideo, processingType);
            
            // then
            assertThat(result).isNotNull();
            assertThat(testMember.getCurrentCredits()).isEqualTo(700); // 1000 - 300 = 700
            
            then(memberAdaptor).should().queryById(1L);
            then(creditHistoryRepository).should().save(any(CreditHistory.class));
        }
        
        @Test
        @DisplayName("실패: 크레딧 부족")
        void useCreditsForVideoProcessing_InsufficientCredits() {
            // given
            Member poorMember = Member.builder()
                    .id(1L)
                    .currentCredits(100)  // 부족한 크레딧
                    .build();
            
            ProcessingType processingType = ProcessingType.BASIC_ENHANCEMENT;
            
            given(memberAdaptor.queryById(1L)).willReturn(poorMember);
            
            // when & then
            assertThatThrownBy(() -> 
                    creditService.useCreditsForVideoProcessing(testVideo, processingType))
                    .isInstanceOf(CreditHandler.class);
            
            // 크레딧이 차감되지 않았는지 확인
            assertThat(poorMember.getCurrentCredits()).isEqualTo(100);
            
            // Repository save가 호출되지 않았는지 확인
            then(creditHistoryRepository).should(never()).save(any());
        }
        
        @Test
        @DisplayName("크레딧 계산 검증: BASIC_ENHANCEMENT")
        void calculateCredits_BasicEnhancement() {
            // given
            ProcessingType processingType = ProcessingType.BASIC_ENHANCEMENT;
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(any());
            
            // when
            creditService.useCreditsForVideoProcessing(testVideo, processingType);
            
            // then - 300초 * 1 크레딧 = 300 크레딧 차감 확인
            assertThat(testMember.getCurrentCredits()).isEqualTo(700);
        }
        
        @Test
        @DisplayName("크레딧 계산 검증: AI_UPSCALING")
        void calculateCredits_AiUpscaling() {
            // given
            ProcessingType processingType = ProcessingType.AI_UPSCALING;
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(any());
            
            // when
            creditService.useCreditsForVideoProcessing(testVideo, processingType);
            
            // then - 300초 * 3 크레딧 = 900 크레딧 차감 확인
            assertThat(testMember.getCurrentCredits()).isEqualTo(100);
        }
    }
    
    @Nested
    @DisplayName("크레딧 충전 테스트")
    class AddCreditsTest {
        
        @Test
        @DisplayName("성공: 일반 크레딧 충전")
        void addCredits_Success() {
            // given
            String description = "관리자 지급";
            CreditHistory expectedHistory = CreditHistory.createCharge(1L, 500, description);
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(expectedHistory);
            
            // when
            CreditHistory result = creditService.addCredits(1L, 500, description);
            
            // then
            assertThat(result).isNotNull();
            assertThat(testMember.getCurrentCredits()).isEqualTo(1500); // 1000 + 500 = 1500
            
            then(memberAdaptor).should().queryById(1L);
            then(creditHistoryRepository).should().save(any(CreditHistory.class));
        }
        
        @Test
        @DisplayName("성공: 결제를 통한 크레딧 충전")
        void addCreditsFromPayment_Success() {
            // given
            String paymentId = "PAY_1234567890";
            CreditHistory expectedHistory = CreditHistory.createCharge(1L, 1000, "크레딧 구매 (결제ID: " + paymentId + ")");
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(expectedHistory);
            
            // when
            CreditHistory result = creditService.addCreditsFromPayment(1L, 1000, paymentId);
            
            // then
            assertThat(result).isNotNull();
            assertThat(testMember.getCurrentCredits()).isEqualTo(2000); // 1000 + 1000 = 2000
            
            then(memberAdaptor).should().queryById(1L);
            then(creditHistoryRepository).should().save(any(CreditHistory.class));
        }
        
        @Test
        @DisplayName("실패: 잘못된 충전 금액 (0 이하)")
        void addCredits_InvalidAmount() {
            // given
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            
            // when & then
            assertThatThrownBy(() -> 
                    creditService.addCredits(1L, 0, "잘못된 충전"))
                    .isInstanceOf(CreditHandler.class);
            
            assertThatThrownBy(() -> 
                    creditService.addCredits(1L, -100, "잘못된 충전"))
                    .isInstanceOf(CreditHandler.class);
            
            // 크레딧이 변경되지 않았는지 확인
            assertThat(testMember.getCurrentCredits()).isEqualTo(1000);
            
            // Repository save가 호출되지 않았는지 확인
            then(creditHistoryRepository).should(never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("크레딧 환불 테스트")
    class RefundCreditsTest {
        
        @Test
        @DisplayName("성공: 크레딧 환불")
        void refundCredits_Success() {
            // given
            String reason = "영상 처리 실패";
            CreditHistory expectedHistory = CreditHistory.createRefund(1L, 1L, 300, reason);
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(expectedHistory);
            
            // when
            CreditHistory result = creditService.refundCredits(1L, 1L, 300, reason);
            
            // then
            assertThat(result).isNotNull();
            assertThat(testMember.getCurrentCredits()).isEqualTo(1300); // 1000 + 300 = 1300
            
            then(memberAdaptor).should().queryById(1L);
            then(creditHistoryRepository).should().save(any(CreditHistory.class));
        }
        
        @Test
        @DisplayName("실패: 잘못된 환불 금액 (0 이하)")
        void refundCredits_InvalidAmount() {
            // given
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            
            // when & then
            assertThatThrownBy(() -> 
                    creditService.refundCredits(1L, 1L, 0, "잘못된 환불"))
                    .isInstanceOf(CreditHandler.class);
            
            // 크레딧이 변경되지 않았는지 확인
            assertThat(testMember.getCurrentCredits()).isEqualTo(1000);
        }
    }
    
    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryTest {
        
        @Test
        @DisplayName("매우 짧은 영상 (1초)")
        void veryShortVideo() {
            // given
            VideoMetadata shortMetadata = new VideoMetadata(1.0, 1920, 1080, "h264", 5000000L, 30.0);
            Video shortVideo = Video.builder()
                    .id(1L)
                    .memberId(1L)
                    .originalFileName("short-video.mp4")
                    .s3OriginalKey("videos/short-video.mp4")
                    .fileSizeBytes(1000000L)
                    .status(VideoStatus.UPLOADED)
                    .metadata(shortMetadata)
                    .build();
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(any());
            
            // when
            creditService.useCreditsForVideoProcessing(shortVideo, ProcessingType.BASIC_ENHANCEMENT);
            
            // then - Math.ceil(1.0 * 1) = 1 크레딧 차감
            assertThat(testMember.getCurrentCredits()).isEqualTo(999);
        }
        
        @Test
        @DisplayName("매우 긴 영상 (1시간)")
        void veryLongVideo() {
            // given
            VideoMetadata longMetadata = new VideoMetadata(3600.0, 1920, 1080, "h264", 5000000L, 30.0);
            Video longVideo = Video.builder()
                    .id(1L)
                    .memberId(1L)
                    .originalFileName("long-video.mp4")
                    .s3OriginalKey("videos/long-video.mp4")
                    .fileSizeBytes(1000000000L)
                    .status(VideoStatus.UPLOADED)
                    .metadata(longMetadata)
                    .build();
            
            // 충분한 크레딧을 가진 멤버
            Member richMember = Member.builder()
                    .id(1L)
                    .username("richuser")
                    .nickname("부자유저")
                    .email("rich@test.com")
                    .role(Role.USER)
                    .currentCredits(10000)
                    .build();
            
            given(memberAdaptor.queryById(1L)).willReturn(richMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(any());
            
            // when
            creditService.useCreditsForVideoProcessing(longVideo, ProcessingType.BASIC_ENHANCEMENT);
            
            // then - 3600초 * 1 크레딧 = 3600 크레딧 차감
            assertThat(richMember.getCurrentCredits()).isEqualTo(6400);
        }
        
        @Test
        @DisplayName("소수점 영상 길이 올림 처리")
        void fractionalVideoDuration() {
            // given
            VideoMetadata fractionalMetadata = new VideoMetadata(1.7, 1920, 1080, "h264", 5000000L, 30.0);
            Video fractionalVideo = Video.builder()
                    .id(1L)
                    .memberId(1L)
                    .originalFileName("fractional-video.mp4")
                    .s3OriginalKey("videos/fractional-video.mp4")
                    .fileSizeBytes(5000000L)
                    .status(VideoStatus.UPLOADED)
                    .metadata(fractionalMetadata)
                    .build();
            
            given(memberAdaptor.queryById(1L)).willReturn(testMember);
            given(creditHistoryRepository.save(any(CreditHistory.class))).willReturn(any());
            
            // when
            creditService.useCreditsForVideoProcessing(fractionalVideo, ProcessingType.BASIC_ENHANCEMENT);
            
            // then - Math.ceil(1.7 * 1) = 2 크레딧 차감
            assertThat(testMember.getCurrentCredits()).isEqualTo(998);
        }
    }
}
