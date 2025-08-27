package com.example.echoshotx.domain.credit.adaptor;

import com.example.echoshotx.domain.credit.entity.CreditHistory;
import com.example.echoshotx.domain.credit.entity.TransactionType;
import com.example.echoshotx.domain.credit.exception.CreditErrorStatus;
import com.example.echoshotx.domain.credit.exception.CreditHandler;
import com.example.echoshotx.domain.credit.repository.CreditHistoryRepository;
import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.infrastructure.annotation.adaptor.Adaptor;
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
     * 회원 크레딧 전체 내역 조회 (사용/충전/환불 모두)
     */
    public List<CreditHistory> queryAllHistory(Member member) {
        return creditHistoryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
    }
    
    /**
     * 회원 크레딧 사용 내역만 조회
     */
    public List<CreditHistory> queryUsageHistory(Member member) {
        return creditHistoryRepository.findByMemberIdAndTransactionTypeOrderByCreatedAtDesc(
                member.getId(), TransactionType.USAGE);
    }
    
    /**
     * 회원 크레딧 충전 내역만 조회
     */
    public List<CreditHistory> queryChargeHistory(Member member) {
        return creditHistoryRepository.findByMemberIdAndTransactionTypeOrderByCreatedAtDesc(
                member.getId(), TransactionType.CHARGE);
    }
    
    /**
     * 기간별 크레딧 내역 조회
     */
    public List<CreditHistory> queryHistoryByDateRange(Member member,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        
        return creditHistoryRepository.findByMemberIdAndCreatedAtBetween(
                member.getId(), startDate, endDate);
    }
    
    /**
     * 최근 N개 크레딧 내역 조회
     */
    public List<CreditHistory> queryRecentHistory(Member member, int limit) {
        validateQueryLimit(limit);
        
        return creditHistoryRepository.findRecentHistory(member.getId(), limit);
    }
    
    /**
     * 특정 타입별 총 사용/충전 금액 조회
     */
    public Integer queryTotalAmountByType(Member member, TransactionType transactionType) {
        Integer total = creditHistoryRepository.sumAmountByMemberAndType(member.getId(), transactionType);
        return total != null ? total : 0;
    }
    
    /**
     * 조회 개수 유효성 검증
     */
    private void validateQueryLimit(int limit) {
        if (limit <= 0) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_QUERY_LIMIT);
        }
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
