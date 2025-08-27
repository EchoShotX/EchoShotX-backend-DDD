package com.example.echoshotx.domain.credit.repository;

import com.example.echoshotx.domain.credit.entity.CreditHistory;
import com.example.echoshotx.domain.credit.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {
    
    /**
     * 회원별 크레딧 내역 조회 (최신순)
     */
    List<CreditHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    /**
     * 회원별 특정 타입 내역 조회
     */
    List<CreditHistory> findByMemberIdAndTransactionTypeOrderByCreatedAtDesc(Long memberId, 
                                                                            TransactionType transactionType);
    
    /**
     * 기간별 크레딧 내역 조회
     */
    List<CreditHistory> findByMemberIdAndCreatedAtBetween(Long memberId, 
                                                         LocalDateTime startDate, 
                                                         LocalDateTime endDate);
    
    /**
     * 최근 N개 크레딧 내역 조회
     */
    @Query("SELECT ch FROM CreditHistory ch WHERE ch.memberId = :memberId " +
           "ORDER BY ch.createdAt DESC LIMIT :limit")
    List<CreditHistory> findRecentHistory(@Param("memberId") Long memberId, 
                                        @Param("limit") int limit);
    
    /**
     * 특정 타입별 총 금액 조회
     */
    @Query("SELECT SUM(ch.amount) FROM CreditHistory ch " +
           "WHERE ch.memberId = :memberId AND ch.transactionType = :type")
    Integer sumAmountByMemberAndType(@Param("memberId") Long memberId, 
                                   @Param("type") TransactionType type);
}
