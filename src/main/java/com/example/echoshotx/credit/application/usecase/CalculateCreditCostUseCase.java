package com.example.echoshotx.credit.application.usecase;

import com.example.echoshotx.credit.domain.util.CreditCalculator;
import com.example.echoshotx.credit.presentation.dto.request.CalculateCreditCostRequest;
import com.example.echoshotx.credit.presentation.dto.response.CreditCostResponse;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 크레딧 소모량 계산 UseCase.
 *
 * <p>비디오 처리에 필요한 크레딧을 계산하여 사용자에게 반환합니다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class CalculateCreditCostUseCase {

    /**
     * 비디오 처리에 필요한 크레딧을 계산합니다.
     *
     * @param request 크레딧 계산 요청 (처리 타입, 영상 길이)
     * @return 크레딧 계산 결과
     */
    public CreditCostResponse execute(CalculateCreditCostRequest request) {
        log.debug("Calculating credit cost: processingType={}, durationSeconds={}",
                request.getProcessingType(), request.getDurationSeconds());

        int requiredCredits = CreditCalculator.calculateRequiredCredits(
                request.getProcessingType(),
                request.getDurationSeconds()
        );

        CreditCostResponse response = CreditCostResponse.of(
                request.getProcessingType(),
                request.getDurationSeconds(),
                requiredCredits
        );

        log.debug("Credit cost calculated: requiredCredits={}", requiredCredits);
        return response;
    }
}
