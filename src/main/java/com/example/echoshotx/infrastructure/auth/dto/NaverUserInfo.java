package com.example.demo.infrastructure.auth.dto;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo {
    private final Map<String, Object> responseMap;

    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
        this.responseMap = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getSocialId() {
        return responseMap.get("id").toString();
    }

    @Override
    public String getEmail() {
        return responseMap.get("email").toString();
    }

}