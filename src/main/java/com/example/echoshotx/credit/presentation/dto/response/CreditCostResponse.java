package com.example.echoshotx.credit.presentation.dto.response;

import com.example.echoshotx.video.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCostResponse {

    private ProcessingType processingType;
    private String processingTypeDescription;
    private Double durationSeconds;
    private Integer creditCostPerSecond;
    private Integer requiredCredits;
    private String calculationFormula;

    /**
     * 크레딧 계산 결과를 Response DTO로 변환합니다.
     *
     * @param processingType 처리 타입
     * @param durationSeconds 영상 길이 (초)
     * @param requiredCredits 필요한 크레딧
     * @return CreditCostResponse
     */
    public static CreditCostResponse of(
            ProcessingType processingType,
            Double durationSeconds,
            Integer requiredCredits
    ) {
        int costPerSecond = processingType.getCreditCostPerSecond();
        String formula = String.format("ceil(%d credits/sec × %.2f sec) = %d credits",
                costPerSecond, durationSeconds, requiredCredits);

        return CreditCostResponse.builder()
                .processingType(processingType)
                .processingTypeDescription(processingType.getDescription())
                .durationSeconds(durationSeconds)
                .creditCostPerSecond(costPerSecond)
                .requiredCredits(requiredCredits)
                .calculationFormula(formula)
                .build();
    }
}
