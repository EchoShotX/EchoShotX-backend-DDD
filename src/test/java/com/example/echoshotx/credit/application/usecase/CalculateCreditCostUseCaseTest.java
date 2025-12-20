package com.example.echoshotx.credit.application.usecase;

import com.example.echoshotx.credit.domain.exception.CreditErrorStatus;
import com.example.echoshotx.credit.presentation.dto.request.CalculateCreditCostRequest;
import com.example.echoshotx.credit.presentation.dto.response.CreditCostResponse;
import com.example.echoshotx.credit.presentation.exception.CreditHandler;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CalculateCreditCostUseCase 단위 테스트
 *
 * <p>테스트 범위:
 * <ol>
 *   <li>정상적인 크레딧 계산 (BASIC_ENHANCEMENT, AI_UPSCALING)</li>
 *   <li>소수점 처리 (올림)</li>
 *   <li>Response DTO 필드 검증</li>
 *   <li>입력 검증 (null, 0 이하 값)</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalculateCreditCostUseCase 테스트")
class CalculateCreditCostUseCaseTest {

    @InjectMocks
    private CalculateCreditCostUseCase calculateCreditCostUseCase;

    @Nested
    @DisplayName("execute 메서드 - 성공 케이스")
    class SuccessTest {

        @Test
        @DisplayName("성공: BASIC_ENHANCEMENT - 정수 초")
        void execute_Success_WhenBasicEnhancementWithIntegerSeconds() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(60.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.BASIC_ENHANCEMENT);
            assertThat(response.getProcessingTypeDescription()).isEqualTo("기본 향상");
            assertThat(response.getDurationSeconds()).isEqualTo(60.0);
            assertThat(response.getCreditCostPerSecond()).isEqualTo(1);
            assertThat(response.getRequiredCredits()).isEqualTo(60);
            assertThat(response.getCalculationFormula())
                    .isEqualTo("ceil(1 credits/sec × 60.00 sec) = 60 credits");
        }

        @Test
        @DisplayName("성공: AI_UPSCALING - 정수 초")
        void execute_Success_WhenAiUpscalingWithIntegerSeconds() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(60.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProcessingType()).isEqualTo(ProcessingType.AI_UPSCALING);
            assertThat(response.getProcessingTypeDescription()).isEqualTo("AI 업스케일링");
            assertThat(response.getDurationSeconds()).isEqualTo(60.0);
            assertThat(response.getCreditCostPerSecond()).isEqualTo(3);
            assertThat(response.getRequiredCredits()).isEqualTo(180);
            assertThat(response.getCalculationFormula())
                    .isEqualTo("ceil(3 credits/sec × 60.00 sec) = 180 credits");
        }

        @Test
        @DisplayName("성공: BASIC_ENHANCEMENT - 소수점 초 (올림 처리)")
        void execute_Success_WhenBasicEnhancementWithDecimalSeconds() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(10.1);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRequiredCredits()).isEqualTo(11); // ceil(10.1) = 11
            assertThat(response.getCalculationFormula())
                    .isEqualTo("ceil(1 credits/sec × 10.10 sec) = 11 credits");
        }

        @Test
        @DisplayName("성공: AI_UPSCALING - 소수점 초 (올림 처리)")
        void execute_Success_WhenAiUpscalingWithDecimalSeconds() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(10.3);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRequiredCredits()).isEqualTo(31); // ceil(3 * 10.3) = ceil(30.9) = 31
            assertThat(response.getCalculationFormula())
                    .isEqualTo("ceil(3 credits/sec × 10.30 sec) = 31 credits");
        }

        @Test
        @DisplayName("성공: 최소 영상 길이 (0.1초)")
        void execute_Success_WhenMinimumDuration() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(0.1);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRequiredCredits()).isEqualTo(1); // ceil(0.1) = 1
        }

        @Test
        @DisplayName("성공: 긴 영상 (5분)")
        void execute_Success_WhenLongVideo() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(300.0); // 5분

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRequiredCredits()).isEqualTo(900); // 3 * 300 = 900
        }

        @Test
        @DisplayName("성공: Response에 모든 필수 필드가 포함됨")
        void execute_ReturnsResponse_WithAllRequiredFields() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(30.5);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getProcessingType()).isNotNull();
            assertThat(response.getProcessingTypeDescription()).isNotBlank();
            assertThat(response.getDurationSeconds()).isNotNull();
            assertThat(response.getCreditCostPerSecond()).isNotNull();
            assertThat(response.getRequiredCredits()).isNotNull();
            assertThat(response.getCalculationFormula()).isNotBlank();
        }

        @Test
        @DisplayName("성공: 계산식 포맷 검증")
        void execute_ReturnsResponse_WithCorrectFormulaFormat() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(15.75);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getCalculationFormula())
                    .matches("ceil\\(\\d+ credits/sec × \\d+\\.\\d{2} sec\\) = \\d+ credits");
            assertThat(response.getCalculationFormula())
                    .contains("1 credits/sec")
                    .contains("15.75 sec")
                    .contains("16 credits");
        }
    }

    @Nested
    @DisplayName("execute 메서드 - 실패 케이스")
    class FailureTest {

        @Test
        @DisplayName("실패: null processingType")
        void execute_ThrowsException_WhenProcessingTypeIsNull() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(null);
            request.setDurationSeconds(60.0);

            // When & Then
            assertThatThrownBy(() -> calculateCreditCostUseCase.execute(request))
                    .isInstanceOf(CreditHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", CreditErrorStatus.CREDIT_INVALID_PROCESSING_TYPE);
        }

        @Test
        @DisplayName("실패: null durationSeconds")
        void execute_ThrowsException_WhenDurationSecondsIsNull() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(null);

            // When & Then
            assertThatThrownBy(() -> calculateCreditCostUseCase.execute(request))
                    .isInstanceOf(CreditHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", CreditErrorStatus.CREDIT_INVALID_DURATION);
        }

        @Test
        @DisplayName("실패: 0 이하 durationSeconds")
        void execute_ThrowsException_WhenDurationSecondsIsZero() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(0.0);

            // When & Then
            assertThatThrownBy(() -> calculateCreditCostUseCase.execute(request))
                    .isInstanceOf(CreditHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", CreditErrorStatus.CREDIT_INVALID_DURATION);
        }

        @Test
        @DisplayName("실패: 음수 durationSeconds")
        void execute_ThrowsException_WhenDurationSecondsIsNegative() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(-10.0);

            // When & Then
            assertThatThrownBy(() -> calculateCreditCostUseCase.execute(request))
                    .isInstanceOf(CreditHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", CreditErrorStatus.CREDIT_INVALID_DURATION);
        }

        @Test
        @DisplayName("실패: 매우 작은 음수 durationSeconds")
        void execute_ThrowsException_WhenDurationSecondsIsVerySmallNegative() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(-0.1);

            // When & Then
            assertThatThrownBy(() -> calculateCreditCostUseCase.execute(request))
                    .isInstanceOf(CreditHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", CreditErrorStatus.CREDIT_INVALID_DURATION);
        }
    }

    @Nested
    @DisplayName("다양한 영상 길이 테스트")
    class VariousDurationTest {

        @Test
        @DisplayName("성공: 10초 영상 - BASIC_ENHANCEMENT")
        void execute_Success_When10SecondsBasicEnhancement() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(10.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: 10초 영상 - AI_UPSCALING")
        void execute_Success_When10SecondsAiUpscaling() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(10.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(30);
        }

        @Test
        @DisplayName("성공: 30초 영상 - BASIC_ENHANCEMENT")
        void execute_Success_When30SecondsBasicEnhancement() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(30.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(30);
        }

        @Test
        @DisplayName("성공: 30초 영상 - AI_UPSCALING")
        void execute_Success_When30SecondsAiUpscaling() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(30.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(90);
        }

        @Test
        @DisplayName("성공: 1분 영상 - BASIC_ENHANCEMENT")
        void execute_Success_When1MinuteBasicEnhancement() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.BASIC_ENHANCEMENT);
            request.setDurationSeconds(60.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(60);
        }

        @Test
        @DisplayName("성공: 1분 영상 - AI_UPSCALING")
        void execute_Success_When1MinuteAiUpscaling() {
            // Given
            CalculateCreditCostRequest request = new CalculateCreditCostRequest();
            request.setProcessingType(ProcessingType.AI_UPSCALING);
            request.setDurationSeconds(60.0);

            // When
            CreditCostResponse response = calculateCreditCostUseCase.execute(request);

            // Then
            assertThat(response.getRequiredCredits()).isEqualTo(180);
        }
    }
}
