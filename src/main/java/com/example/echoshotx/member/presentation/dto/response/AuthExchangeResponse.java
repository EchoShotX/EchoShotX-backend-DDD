package com.example.echoshotx.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthExchangeResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // 초 단위, 기본값 1800 (30분)
}

