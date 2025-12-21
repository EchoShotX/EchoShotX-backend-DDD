package com.example.echoshotx.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MemberResponse {

    @Data
    @Builder
    @AllArgsConstructor
    public static class MyInfo {
        // 이메일
        private final String email;
        // 가입 날짜
        private final LocalDateTime joinedAt;
        // 내 크레딧
        private final Integer credit;

    }

}
