package com.example.demo.domain.member.entity;

import com.example.demo.domain.auditing.entity.BaseTimeEntity;
import com.example.demo.domain.member.exception.CreditErrorStatus;
import com.example.demo.domain.member.exception.CreditHandler;
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
                @Index(name = "idx_member_nickname", columnList = "nickname"),
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

    @Column(nullable = false, unique = true)
    private String nickname;

    private String email;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentCredits = 0;


    //business
    private boolean isNotAdmin() {
        return this.role != Role.ADMIN;
    }

    public boolean hasEnoughCredits(int requiredTokens) {
        if(requiredTokens < 0) {
            throw new IllegalArgumentException("requiredTokens must be non-negative");
        }
        return this.currentCredits >= requiredTokens;
    }

    public void useCredits(int amount) {
        if(amount <= 0) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_NOT_VALID);
        }
        if(!hasEnoughCredits(amount)) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_NOT_ENOUGH);
        }
        this.currentCredits -= amount;
    }

    public void addCredits(int amount) {
        if(amount <= 0) {
            throw new CreditHandler(CreditErrorStatus.CREDIT_NOT_VALID);
        }
        this.currentCredits += amount;
    }

}
