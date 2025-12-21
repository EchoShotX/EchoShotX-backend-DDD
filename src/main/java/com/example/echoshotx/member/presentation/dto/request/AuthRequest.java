package com.example.echoshotx.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthRequest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReIssue {
        private String refreshToken;
    }

}
