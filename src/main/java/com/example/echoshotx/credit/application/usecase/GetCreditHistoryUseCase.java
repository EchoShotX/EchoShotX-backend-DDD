package com.example.echoshotx.credit.application.usecase;

import com.example.echoshotx.credit.application.adaptor.CreditAdaptor;
import com.example.echoshotx.credit.domain.entity.CreditHistory;
import com.example.echoshotx.credit.presentation.dto.response.CreditHistoryResponse;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 크레딧 히스토리 조회 UseCase.
 *
 * <p>사용자의 크레딧 사용/충전/환불 내역을 조회합니다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetCreditHistoryUseCase {

    private final CreditAdaptor creditAdaptor;

    /**
     * 사용자의 크레딧 히스토리를 조회합니다.
     *
     * @param member 조회할 회원
     * @return 크레딧 히스토리 목록 (최신순)
     */
    public List<CreditHistoryResponse> execute(Member member) {
        log.debug("Fetching credit history for member: memberId={}", member.getId());

        List<CreditHistory> creditHistories = creditAdaptor.queryAllHistory(member);

        List<CreditHistoryResponse> responses = creditHistories.stream()
                .map(CreditHistoryResponse::from)
                .collect(Collectors.toList());

        log.debug("Credit history fetched: memberId={}, count={}", member.getId(), responses.size());
        return responses;
    }
}
