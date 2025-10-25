package com.example.echoshotx.credit.infrastructure.persistence;

import com.example.echoshotx.credit.domain.entity.CreditHistory;
import com.example.echoshotx.credit.domain.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {
    
    /**
     * 회원별 크레딧 내역 조회 (최신순)
     */
    List<CreditHistory> findByMemberIdOrderByCreatedDateDesc(Long memberId);
    
    /**
     * 회원별 특정 타입 내역 조회
     */
    List<CreditHistory> findByMemberIdAndTransactionTypeOrderByCreatedDateDesc(Long memberId,
                                                                               TransactionType transactionType);

}
