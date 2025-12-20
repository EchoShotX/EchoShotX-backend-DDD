package com.example.echoshotx.credit.presentation.dto.response;

import com.example.echoshotx.credit.domain.entity.CreditHistory;
import com.example.echoshotx.credit.domain.entity.TransactionType;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 크레딧 히스토리 응답 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditHistoryResponse {

    private Long id;
    private TransactionType transactionType;
    private String transactionTypeDescription;
    private Integer amount;
    private Long videoId;
    private ProcessingType processingType;
    private String description;
    private LocalDateTime createdDate;

    /**
     * CreditHistory 엔티티로부터 Response DTO를 생성합니다.
     *
     * @param creditHistory 크레딧 히스토리 엔티티
     * @return CreditHistoryResponse 변환된 응답 DTO
     */
    public static CreditHistoryResponse from(CreditHistory creditHistory) {
        return CreditHistoryResponse.builder()
                .id(creditHistory.getId())
                .transactionType(creditHistory.getTransactionType())
                .transactionTypeDescription(creditHistory.getTransactionType().getDescription())
                .amount(creditHistory.getAmount())
                .videoId(creditHistory.getVideoId())
                .processingType(creditHistory.getProcessingType())
                .description(creditHistory.getDescription())
                .createdDate(creditHistory.getCreatedDate())
                .build();
    }
}
