package com.example.echoshotx.credit.application.adaptor;

import com.example.echoshotx.credit.domain.entity.CreditHistory;
import com.example.echoshotx.credit.domain.entity.TransactionType;
import com.example.echoshotx.credit.domain.exception.CreditErrorStatus;
import com.example.echoshotx.credit.presentation.exception.CreditHandler;
import com.example.echoshotx.credit.infrastructure.persistence.CreditHistoryRepository;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreditAdaptor {

    private final CreditHistoryRepository creditHistoryRepository;

    /**
     *  todo
     *  1. 기간 별 조회
     *  2.
     */

    /**
     * 회원 크레딧 전체 내역 조회 (사용/충전/환불 모두)
     */
    public List<CreditHistory> queryAllHistory(Member member) {
        return creditHistoryRepository.findByMemberIdOrderByCreatedDateDesc(member.getId());
    }
    
    /**
     * 회원 크레딧 사용 내역만 조회
     */
    public List<CreditHistory> queryUsageHistory(Member member) {
        return creditHistoryRepository.findByMemberIdAndTransactionTypeOrderByCreatedDateDesc(
                member.getId(), TransactionType.USAGE);
    }
    
    /**
     * 회원 크레딧 충전 내역만 조회
     */
    public List<CreditHistory> queryChargeHistory(Member member) {
        return creditHistoryRepository.findByMemberIdAndTransactionTypeOrderByCreatedDateDesc(
                member.getId(), TransactionType.CHARGE);
    }

    /**
     * 날짜 범위 유효성 검증
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_DATE_RANGE);
        }
    }

}
