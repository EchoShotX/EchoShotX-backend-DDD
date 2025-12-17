package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.application.adaptor.MemberAdaptor;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.entity.Role;
import com.example.echoshotx.member.domain.exception.MemberErrorStatus;
import com.example.echoshotx.member.presentation.dto.response.AuthExchangeResponse;
import com.example.echoshotx.member.presentation.exception.MemberHandler;
import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;
import com.example.echoshotx.shared.redis.service.RedisService;
import com.example.echoshotx.shared.security.dto.JwtToken;
import com.example.echoshotx.shared.security.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

/**
 * ExchangeCodeUseCase 단위 테스트
 *
 * 테스트 범위:
 * 1. 정상적인 코드 교환 플로우
 * 2. 유효하지 않은 코드 처리
 * 3. Member 조회 실패 처리
 * 4. 코드 1회 사용 보장 (삭제 확인)
 * 5. JWT 토큰 생성 및 반환
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeCodeUseCase 테스트")
class ExchangeCodeUseCaseTest {

    @Mock
    private RedisService redisService;

    @Mock
    private TokenService tokenService;

    @Mock
    private MemberAdaptor memberAdaptor;

    @InjectMocks
    private ExchangeCodeUseCase exchangeCodeUseCase;

    private Member testMember;
    private String validCode;
    private String username;
    private JwtToken testJwtToken;

    @BeforeEach
    void setUp() {
        validCode = "test-code-12345";
        username = "google_123456789";

        testMember = Member.builder()
                .id(1L)
                .username(username)
                .email("test@example.com")
                .role(Role.USER)
                .currentCredits(1000)
                .build();

        testJwtToken = JwtToken.builder()
                .grantType("Bearer")
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .build();
    }

    @Nested
    @DisplayName("execute 메서드 - 성공 케이스")
    class SuccessTest {

        @Test
        @DisplayName("성공: 유효한 코드로 JWT 토큰 교환")
        void execute_Success_WhenValidCode() {
            // Given
            given(redisService.getAuthCode(validCode)).willReturn(username);
            given(memberAdaptor.queryByUsername(username)).willReturn(testMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            AuthExchangeResponse response = exchangeCodeUseCase.execute(validCode);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-123");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getExpiresIn()).isEqualTo(1800); // 30분

            // 코드가 삭제되었는지 확인 (1회 사용 보장)
            verify(redisService).deleteAuthCode(validCode);
        }

        @Test
        @DisplayName("성공: JWT 토큰 생성 시 올바른 Authentication 객체 전달")
        void execute_CallsTokenService_WithCorrectAuthentication() {
            // Given
            given(redisService.getAuthCode(validCode)).willReturn(username);
            given(memberAdaptor.queryByUsername(username)).willReturn(testMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            ArgumentCaptor<org.springframework.security.core.Authentication> authCaptor =
                    ArgumentCaptor.forClass(org.springframework.security.core.Authentication.class);

            // When
            exchangeCodeUseCase.execute(validCode);

            // Then
            verify(tokenService).generateToken(authCaptor.capture());
            org.springframework.security.core.Authentication capturedAuth = authCaptor.getValue();
            
            assertThat(capturedAuth).isNotNull();
            assertThat(capturedAuth.getName()).isEqualTo(testMember.getId().toString());
            assertThat(capturedAuth.getAuthorities()).hasSize(1);
            assertThat(capturedAuth.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("성공: 코드 교환 후 Redis에서 코드가 삭제됨 (1회 사용 보장)")
        void execute_DeletesCode_AfterSuccessfulExchange() {
            // Given
            given(redisService.getAuthCode(validCode)).willReturn(username);
            given(memberAdaptor.queryByUsername(username)).willReturn(testMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            exchangeCodeUseCase.execute(validCode);

            // Then
            verify(redisService).getAuthCode(validCode);
            verify(redisService).deleteAuthCode(validCode);
            
            // 호출 순서 확인: 조회 후 삭제
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(redisService);
            inOrder.verify(redisService).getAuthCode(validCode);
            inOrder.verify(redisService).deleteAuthCode(validCode);
        }

        @Test
        @DisplayName("성공: Response에 모든 필수 필드가 포함됨")
        void execute_ReturnsResponse_WithAllRequiredFields() {
            // Given
            given(redisService.getAuthCode(validCode)).willReturn(username);
            given(memberAdaptor.queryByUsername(username)).willReturn(testMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            AuthExchangeResponse response = exchangeCodeUseCase.execute(validCode);

            // Then
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getExpiresIn()).isEqualTo(1800);
        }
    }

    @Nested
    @DisplayName("execute 메서드 - 실패 케이스")
    class FailureTest {

        @Test
        @DisplayName("실패: 유효하지 않은 코드 (Redis에서 null 반환)")
        void execute_ThrowsException_WhenCodeIsInvalid() {
            // Given
            String invalidCode = "invalid-code";
            given(redisService.getAuthCode(invalidCode)).willReturn(null);

            // When & Then
            assertThatThrownBy(() -> exchangeCodeUseCase.execute(invalidCode))
                    .isInstanceOf(MemberHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", ErrorStatus.AUTH_CODE_INVALID);

            // 코드 삭제가 호출되지 않아야 함
            verify(redisService, never()).deleteAuthCode(anyString());
            verify(tokenService, never()).generateToken(any());
        }

        @Test
        @DisplayName("실패: 만료된 코드 (Redis에서 null 반환)")
        void execute_ThrowsException_WhenCodeIsExpired() {
            // Given
            String expiredCode = "expired-code";
            given(redisService.getAuthCode(expiredCode)).willReturn(null);

            // When & Then
            assertThatThrownBy(() -> exchangeCodeUseCase.execute(expiredCode))
                    .isInstanceOf(MemberHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", ErrorStatus.AUTH_CODE_INVALID);

            verify(redisService, never()).deleteAuthCode(anyString());
            verify(tokenService, never()).generateToken(any());
        }

        @Test
        @DisplayName("실패: Member를 찾을 수 없음")
        void execute_ThrowsException_WhenMemberNotFound() {
            // Given
            String nonExistentUsername = "non-existent-user";
            given(redisService.getAuthCode(validCode)).willReturn(nonExistentUsername);
            given(memberAdaptor.queryByUsername(nonExistentUsername))
                    .willThrow(new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> exchangeCodeUseCase.execute(validCode))
                    .isInstanceOf(MemberHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", MemberErrorStatus.MEMBER_NOT_FOUND);

            // Member를 찾지 못했으므로 토큰 생성 및 코드 삭제가 호출되지 않아야 함
            verify(tokenService, never()).generateToken(any());
            verify(redisService, never()).deleteAuthCode(anyString());
        }

        @Test
        @DisplayName("실패: 빈 문자열 코드")
        void execute_ThrowsException_WhenCodeIsEmpty() {
            // Given
            String emptyCode = "";
            given(redisService.getAuthCode(emptyCode)).willReturn(null);

            // When & Then
            assertThatThrownBy(() -> exchangeCodeUseCase.execute(emptyCode))
                    .isInstanceOf(MemberHandler.class)
                    .hasFieldOrPropertyWithValue("errorStatus", ErrorStatus.AUTH_CODE_INVALID);
        }
    }

    @Nested
    @DisplayName("다양한 Role 테스트")
    class DifferentRoleTest {

        @Test
        @DisplayName("성공: USER 권한으로 코드 교환")
        void execute_Success_WithUserRole() {
            // Given
            Member userMember = Member.builder()
                    .id(2L)
                    .username("user@example.com")
                    .email("user@example.com")
                    .role(Role.USER)
                    .currentCredits(100)
                    .build();

            given(redisService.getAuthCode(validCode)).willReturn(userMember.getUsername());
            given(memberAdaptor.queryByUsername(userMember.getUsername())).willReturn(userMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            AuthExchangeResponse response = exchangeCodeUseCase.execute(validCode);

            // Then
            assertThat(response).isNotNull();
            verify(tokenService).generateToken(argThat(auth ->
                    auth.getAuthorities().iterator().next().getAuthority().equals("ROLE_USER")
            ));
        }

        @Test
        @DisplayName("성공: ADMIN 권한으로 코드 교환 (코드에서는 항상 ROLE_USER로 생성)")
        void execute_Success_WithAdminRole() {
            // Given
            Member adminMember = Member.builder()
                    .id(3L)
                    .username("admin@example.com")
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .currentCredits(10000)
                    .build();

            given(redisService.getAuthCode(validCode)).willReturn(adminMember.getUsername());
            given(memberAdaptor.queryByUsername(adminMember.getUsername())).willReturn(adminMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            AuthExchangeResponse response = exchangeCodeUseCase.execute(validCode);

            // Then
            assertThat(response).isNotNull();
            // 현재 구현에서는 항상 ROLE_USER로 생성됨
            verify(tokenService).generateToken(argThat(auth ->
                    auth.getAuthorities().iterator().next().getAuthority().equals("ROLE_USER")
            ));
        }
    }

    @Nested
    @DisplayName("메서드 호출 순서 검증")
    class MethodCallOrderTest {

        @Test
        @DisplayName("성공: 메서드 호출 순서가 올바름 (코드 조회 -> Member 조회 -> 토큰 생성 -> 코드 삭제)")
        void execute_CallsMethodsInCorrectOrder() {
            // Given
            given(redisService.getAuthCode(validCode)).willReturn(username);
            given(memberAdaptor.queryByUsername(username)).willReturn(testMember);
            given(tokenService.generateToken(any())).willReturn(testJwtToken);

            // When
            exchangeCodeUseCase.execute(validCode);

            // Then
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(redisService, memberAdaptor, tokenService);
            inOrder.verify(redisService).getAuthCode(validCode);
            inOrder.verify(memberAdaptor).queryByUsername(username);
            inOrder.verify(tokenService).generateToken(any());
            inOrder.verify(redisService).deleteAuthCode(validCode);
        }
    }
}

