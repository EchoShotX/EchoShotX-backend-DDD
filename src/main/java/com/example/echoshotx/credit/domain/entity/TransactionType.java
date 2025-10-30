package com.example.echoshotx.credit.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
    USAGE("사용"),
    CHARGE("충전"),
    REFUND("환불");
    
    private final String description;
}
