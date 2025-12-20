package com.example.echoshotx.credit.presentation.dto.request;

import com.example.echoshotx.video.domain.entity.ProcessingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CalculateCreditCostRequest {

    @NotNull(message = "처리 타입은 필수입니다")
    private ProcessingType processingType;

    @NotNull(message = "영상 길이는 필수입니다")
    @DecimalMin(value = "0.1", message = "영상 길이는 0.1초 이상이어야 합니다")
    private Double durationSeconds;
}
