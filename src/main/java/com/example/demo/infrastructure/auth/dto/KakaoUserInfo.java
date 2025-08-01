package com.example.demo.infrastructure.auth.dto;
import java.util.Map;

public class KakaoUserInfo extends OAuth2UserInfo {
    private final Map<String, Object> account;

    public KakaoUserInfo(Map<String, Object> attributes) {
        super(attributes);
        this.account = (Map<String, Object>) attributes.get("kakao_account");
    }

    @Override
    public String getSocialId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        return account.get("email").toString();
    }
}