package com.example.demo.infrastructure.auth.domain;

import com.example.demo.infrastructure.auth.dto.OAuth2UserInfo;
import com.example.demo.infrastructure.auth.utils.OAuth2Utils;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuth2Attributes {
    private String nameAttributeKey;
    private OAuth2UserInfo oAuth2UserInfo;

    @Builder
    public OAuth2Attributes(String nameAttributeKey, OAuth2UserInfo oAuth2UserInfo) {
        this.nameAttributeKey = nameAttributeKey;
        this.oAuth2UserInfo = oAuth2UserInfo;
    }

    // OAuth2Utils 를 통해 분기처리 없이 생성된 OAuth2UserInfo 를 반환
    public static OAuth2Attributes of(SocialType socialType, String userNameAttributeName,
                                      Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2UserInfo(OAuth2Utils.getOAuth2UserInfo(socialType, attributes))
                .build();
    }
}