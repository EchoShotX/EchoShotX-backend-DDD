package com.example.echoshotx.member.presentation.controller;

import com.example.echoshotx.member.application.usecase.ExchangeCodeUseCase;
import com.example.echoshotx.member.presentation.dto.request.AuthExchangeRequest;
import com.example.echoshotx.member.presentation.dto.response.AuthExchangeResponse;
import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 테스트
 *
 * 테스트 범위:
 * 1. POST /auth/exchange 엔드포인트 정상 동작
 * 2. 요청 검증 (code 필수)
 * 3. UseCase 예외 처리
 * 4. 응답 형식 검증
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeCodeUseCase exchangeCodeUseCase;

    @Nested
    @DisplayName("POST /auth/exchange - 성공 케이스")
    class SuccessTest {

        @Test
        @DisplayName("성공: 유효한 코드로 토큰 교환 요청")
        void exchangeCode_Success_WhenValidCode() throws Exception {
            // Given
            String validCode = "test-code-12345";
            AuthExchangeRequest request = new AuthExchangeRequest(validCode);
            
            AuthExchangeResponse response = AuthExchangeResponse.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .expiresIn(1800)
                    .build();

            given(exchangeCodeUseCase.execute(validCode)).willReturn(response);

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.result.accessToken").value("access-token-123"))
                    .andExpect(jsonPath("$.result.refreshToken").value("refresh-token-456"))
                    .andExpect(jsonPath("$.result.expiresIn").value(1800));
        }
    }

    @Nested
    @DisplayName("POST /auth/exchange - 검증 실패 케이스")
    class ValidationFailureTest {

        @Test
        @DisplayName("실패: code가 null인 경우")
        void exchangeCode_Fails_WhenCodeIsNull() throws Exception {
            // Given
            AuthExchangeRequest request = new AuthExchangeRequest(null);

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: code가 빈 문자열인 경우")
        void exchangeCode_Fails_WhenCodeIsEmpty() throws Exception {
            // Given
            AuthExchangeRequest request = new AuthExchangeRequest("");

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: code 필드가 없는 경우")
        void exchangeCode_Fails_WhenCodeFieldMissing() throws Exception {
            // Given
            String requestBody = "{}";

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: Content-Type이 없는 경우")
        void exchangeCode_Fails_WhenContentTypeMissing() throws Exception {
            // Given
            AuthExchangeRequest request = new AuthExchangeRequest("test-code");

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("POST /auth/exchange - UseCase 예외 처리")
    class UseCaseExceptionTest {

        @Test
        @DisplayName("실패: 유효하지 않은 코드 (UseCase에서 예외 발생)")
        void exchangeCode_ReturnsError_WhenInvalidCode() throws Exception {
            // Given
            String invalidCode = "invalid-code";
            AuthExchangeRequest request = new AuthExchangeRequest(invalidCode);

            given(exchangeCodeUseCase.execute(invalidCode))
                    .willThrow(new com.example.echoshotx.member.presentation.exception.MemberHandler(
                            ErrorStatus.AUTH_CODE_INVALID));

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorStatus.AUTH_CODE_INVALID.getCode()))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("실패: 만료된 코드")
        void exchangeCode_ReturnsError_WhenCodeExpired() throws Exception {
            // Given
            String expiredCode = "expired-code";
            AuthExchangeRequest request = new AuthExchangeRequest(expiredCode);

            given(exchangeCodeUseCase.execute(expiredCode))
                    .willThrow(new com.example.echoshotx.member.presentation.exception.MemberHandler(
                            ErrorStatus.AUTH_CODE_INVALID));

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorStatus.AUTH_CODE_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("응답 형식 검증")
    class ResponseFormatTest {

        @Test
        @DisplayName("성공: 응답에 모든 필수 필드가 포함됨")
        void exchangeCode_ReturnsResponse_WithAllRequiredFields() throws Exception {
            // Given
            String validCode = "test-code";
            AuthExchangeRequest request = new AuthExchangeRequest(validCode);
            
            AuthExchangeResponse response = AuthExchangeResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .expiresIn(1800)
                    .build();

            given(exchangeCodeUseCase.execute(validCode)).willReturn(response);

            // When & Then
            mockMvc.perform(post("/auth/exchange")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").exists())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result.accessToken").exists())
                    .andExpect(jsonPath("$.result.refreshToken").exists())
                    .andExpect(jsonPath("$.result.expiresIn").exists());
        }
    }
}

