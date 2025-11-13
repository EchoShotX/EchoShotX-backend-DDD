package com.example.echoshotx.member.domain.entity;

import com.example.echoshotx.shared.common.BaseTimeEntity;
import com.example.echoshotx.credit.domain.exception.CreditErrorStatus;
import com.example.echoshotx.credit.presentation.exception.CreditHandler;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member",
        indexes = {
                @Index(name = "idx_member_username", columnList = "username"),
//                @Index(name = "idx_member_nickname", columnList = "nickname"),
        }
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, unique = true)
    private String username;

//    @Column(nullable = false, unique = true)
//    private String nickname;

    private String email;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentCredits = 0;


    //business
    private boolean isNotAdmin() {
        return this.role != Role.ADMIN;
    }

    public boolean hasEnoughCredits(int requiredCredits) {
        validateRequiredCredits(requiredCredits);
        return this.currentCredits >= requiredCredits;
    }

    private void validateRequiredCredits(int requiredCredits) {
        if(requiredCredits < 0) {
            throw new IllegalArgumentException("requiredTokens must be non-negative");
        }
    }

    public void useCredits(int amount) {
        validateUseCreditAmount(amount);
        this.currentCredits -= amount;
    }

    private void validateUseCreditAmount(int amount) {
        checkAmountIsPositive(amount);
        validateSufficientCredits(amount);
    }

    private void validateSufficientCredits(int amount) {
        if(!hasEnoughCredits(amount)) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_NOT_ENOUGH);
        }
    }

    private void checkAmountIsPositive(int amount) {
        if(amount <= 0) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_NOT_VALID);
        }
    }

    public void addCredits(int amount) {
        checkAmountIsPositive(amount);
        this.currentCredits += amount;
    }

}
