package com.example.echoshotx.credit.presentation.controller;

import com.example.echoshotx.credit.application.usecase.CalculateCreditCostUseCase;
import com.example.echoshotx.credit.presentation.dto.request.CalculateCreditCostRequest;
import com.example.echoshotx.credit.presentation.dto.response.CreditCostResponse;
import com.example.echoshotx.shared.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 크레딧 관련 API 컨트롤러.
 */
@Slf4j
@Tag(name = "Credit", description = "크레딧 관리 API")
@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CalculateCreditCostUseCase calculateCreditCostUseCase;

    /**
     * 비디오 처리에 필요한 크레딧을 계산합니다.
     *
     * <p>처리 타입과 영상 길이를 입력받아 필요한 크레딧을 계산하여 반환합니다.
     * 이 API는 인증 없이도 접근 가능하며, 사용자가 업로드 전에 예상 비용을 확인할 수 있습니다.
     *
     * @param request 크레딧 계산 요청
     * @return 크레딧 계산 결과
     */
    @Operation(
            summary = "크레딧 소모량 계산",
            description = "비디오 처리에 필요한 크레딧을 계산합니다. " +
                    "처리 타입(BASIC_ENHANCEMENT, AI_UPSCALING)과 영상 길이(초)를 입력받아 " +
                    "필요한 크레딧을 반환합니다."
    )
    @PostMapping("/calculate")
    public ApiResponseDto<CreditCostResponse> calculateCreditCost(
            @Valid @RequestBody CalculateCreditCostRequest request
    ) {
        log.info("Credit cost calculation requested: processingType={}, durationSeconds={}",
                request.getProcessingType(), request.getDurationSeconds());

        CreditCostResponse response = calculateCreditCostUseCase.execute(request);
        return ApiResponseDto.onSuccess(response);
    }
}
