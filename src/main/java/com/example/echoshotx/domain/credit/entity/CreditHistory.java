package com.example.echoshotx.domain.credit.entity;

import com.example.echoshotx.domain.auditing.entity.BaseTimeEntity;
import com.example.echoshotx.domain.video.entity.ProcessingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "credit_history",
    indexes = {
        @Index(name = "idx_credit_history_member_id", columnList = "member_id"),
        @Index(name = "idx_credit_history_created_at", columnList = "created_at"),
        @Index(name = "idx_credit_history_transaction_type", columnList = "transaction_type")
    }
)
public class CreditHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer amount;  // 항상 양수

    @Column(name = "video_id")
    private Long videoId;

    @Enumerated(EnumType.STRING)
    @Column
    private ProcessingType processingType;

    @Column(length = 500)
    private String description;

    // business
    
    /**
     * 크레딧 사용 내역 생성
     */
    public static CreditHistory createUsage(Long memberId, Long videoId, 
                                          Integer amount, ProcessingType processingType) {
        String description = String.format("%s 영상 처리", processingType.getDescription());
        
        return CreditHistory.builder()
                .memberId(memberId)
                .transactionType(TransactionType.USAGE)
                .amount(amount)
                .videoId(videoId)
                .processingType(processingType)
                .description(description)
                .build();
    }
    
    /**
     * 크레딧 충전 내역 생성
     */
    public static CreditHistory createCharge(Long memberId, Integer amount, String description) {
        return CreditHistory.builder()
                .memberId(memberId)
                .transactionType(TransactionType.CHARGE)
                .amount(amount)
                .videoId(null)
                .processingType(null)
                .description(description)
                .build();
    }
    
    /**
     * 크레딧 환불 내역 생성
     */
    public static CreditHistory createRefund(Long memberId, Long videoId, Integer amount, String reason) {
        String description = String.format("크레딧 환불: %s", reason);
        
        return CreditHistory.builder()
                .memberId(memberId)
                .transactionType(TransactionType.REFUND)
                .amount(amount)
                .videoId(videoId)
                .processingType(null)
                .description(description)
                .build();
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
