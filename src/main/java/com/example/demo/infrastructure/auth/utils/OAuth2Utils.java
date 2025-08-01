package com.example.demo.infrastructure.auth.utils;

import com.example.demo.infrastructure.auth.domain.SocialType;
import com.example.demo.infrastructure.auth.dto.GoogleUserInfo;
import com.example.demo.infrastructure.auth.dto.KakaoUserInfo;
import com.example.demo.infrastructure.auth.dto.NaverUserInfo;
import com.example.demo.infrastructure.auth.dto.OAuth2UserInfo;

import java.util.Map;

public class OAuth2Utils {

    // registrationId에 따른 Provider 추출
    public static SocialType getSocialType(String registrationId){

        if(registrationId.equals("naver")){
            return SocialType.NAVER;
        }
        else if(registrationId.equals("google")){
            return SocialType.GOOGLE;
        }
        else if(registrationId.equals("kakao")){
            return SocialType.KAKAO;
        }
        return null;
    }

    public static OAuth2UserInfo getOAuth2UserInfo(SocialType socialType, Map<String, Object> attributes){
        switch (socialType){
            case NAVER:
                return new NaverUserInfo(attributes);
            case GOOGLE:
                return new GoogleUserInfo(attributes);
            case KAKAO:
                return new KakaoUserInfo(attributes);
        }
        return null;
    }
}