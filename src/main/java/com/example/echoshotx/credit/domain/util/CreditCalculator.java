package com.example.echoshotx.credit.domain.util;

import com.example.echoshotx.credit.domain.exception.CreditErrorStatus;
import com.example.echoshotx.credit.presentation.exception.CreditHandler;
import com.example.echoshotx.video.domain.entity.ProcessingType;

/**
 * 크레딧 계산을 담당하는 유틸리티 클래스.
 *
 * <p>비디오 처리에 필요한 크레딧을 계산하는 순수 함수를 제공합니다.
 */
public class CreditCalculator {

    private CreditCalculator() {
        // 유틸리티 클래스는 인스턴스화 불가
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * 비디오 처리에 필요한 크레딧을 계산합니다.
     *
     * <p>계산 공식: Math.ceil(creditCostPerSecond * durationSeconds)
     *
     * @param processingType 처리 타입 (BASIC_ENHANCEMENT, AI_UPSCALING 등)
     * @param durationSeconds 영상 길이 (초 단위)
     * @return 필요한 크레딧 수 (올림 처리된 정수값)
     * @throws CreditHandler durationSeconds가 null이거나 0 이하일 경우
     */
    public static int calculateRequiredCredits(ProcessingType processingType, Double durationSeconds) {
        if (processingType == null) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_PROCESSING_TYPE);
        }

        if (durationSeconds == null || durationSeconds <= 0) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_DURATION);
        }

        double costPerSecond = processingType.getCreditCostPerSecond();
        double totalCost = costPerSecond * durationSeconds;
        return (int) Math.ceil(totalCost);
    }
}
