package com.sofit.admin.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.admin.domain.auth.service.LoginAttemptService;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {

    @InjectMocks
    private AdminAuthServiceImpl adminAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    // ===== login 테스트 =====

    @Nested
    @DisplayName("login")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 시 AdminLoginResponse를 반환한다")
        void login_정상_로그인시_AdminLoginResponse_반환() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            User bankAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("bankadmin", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(bankAdmin));
            given(passwordEncoder.matches("password1!", bankAdmin.getPasswordHash())).willReturn(true);

            // when
            AdminLoginResponse response = adminAuthService.login(request, httpRequest, httpResponse);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("은행원");
            verify(loginAttemptService).loginSucceeded("bankadmin");
        }

        @Test
        @DisplayName("계정/IP 잠금 상태에서 로그인 시 ACCOUNT_LOCKED 예외가 발생한다")
        void login_잠금_상태에서_로그인시_ACCOUNT_LOCKED_예외_발생() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("bankadmin", "127.0.0.1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.ACCOUNT_LOCKED));
        }

        @Test
        @DisplayName("존재하지 않는 아이디로 로그인 시 LOGIN_FAILED 예외가 발생하고 실패 횟수가 증가한다")
        void login_존재하지_않는_아이디_LOGIN_FAILED_예외_발생_및_실패횟수_증가() {
            // given
            AdminLoginRequest request = createLoginRequest("unknown", "password1!");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("unknown", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.LOGIN_FAILED));

            verify(loginAttemptService).loginFailed("unknown", "127.0.0.1");
        }

        @Test
        @DisplayName("INACTIVE 계정으로 로그인 시 LOGIN_FAILED 예외가 발생한다")
        void login_INACTIVE_계정_LOGIN_FAILED_예외_발생() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            User inactiveAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            inactiveAdmin.inactivate();
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("bankadmin", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(inactiveAdmin));

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.LOGIN_FAILED));

            verify(loginAttemptService).loginFailed("bankadmin", "127.0.0.1");
        }

        @Test
        @DisplayName("USER 역할로 어드민 로그인 시도 시 LOGIN_FAILED 예외가 발생한다")
        void login_USER_역할_로그인시도_LOGIN_FAILED_예외_발생() {
            // given
            AdminLoginRequest request = createLoginRequest("normaluser", "password1!");
            User normalUser = createActiveUser(2L, "normaluser", "일반사용자");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("normaluser", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("normaluser")).willReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.LOGIN_FAILED));

            verify(loginAttemptService).loginFailed("normaluser", "127.0.0.1");
        }

        @Test
        @DisplayName("비밀번호 불일치 시 LOGIN_FAILED 예외가 발생하고 실패 횟수가 증가한다")
        void login_비밀번호_불일치시_LOGIN_FAILED_예외_발생_및_실패횟수_증가() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "wrongpass");
            User bankAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("bankadmin", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(bankAdmin));
            given(passwordEncoder.matches("wrongpass", bankAdmin.getPasswordHash())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.LOGIN_FAILED));

            verify(loginAttemptService).loginFailed("bankadmin", "127.0.0.1");
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 해당 IP를 클라이언트 IP로 사용한다")
        void login_X_Forwarded_For_헤더_있으면_해당_IP_사용() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            User bankAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn("10.0.0.1, 192.168.1.1");
            given(loginAttemptService.isBlocked("bankadmin", "10.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(bankAdmin));
            given(passwordEncoder.matches("password1!", bankAdmin.getPasswordHash())).willReturn(true);

            // when
            adminAuthService.login(request, httpRequest, httpResponse);

            // then
            verify(loginAttemptService).loginSucceeded("bankadmin");
            verify(loginAttemptService).isBlocked("bankadmin", "10.0.0.1");
        }
    }

    // ===== logout 테스트 =====

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 시 SessionUtil.invalidateSession이 호출된다")
        void logout_호출시_세션_무효화() {
            // given
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            try (var sessionUtilMock = org.mockito.Mockito.mockStatic(com.sofit.admin.global.util.SessionUtil.class)) {
                // when
                adminAuthService.logout(httpRequest, httpResponse);

                // then
                sessionUtilMock.verify(() -> com.sofit.admin.global.util.SessionUtil.invalidateSession(httpRequest, httpResponse));
            }
        }
    }

    // ===== findMe 테스트 =====

    @Nested
    @DisplayName("findMe")
    class FindMeTest {

        @Test
        @DisplayName("정상 조회 시 AdminMeResponse를 반환한다")
        void findMe_정상_조회시_AdminMeResponse_반환() {
            // given
            User adminUser = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);

            try (var securityUtilMock = org.mockito.Mockito.mockStatic(com.sofit.admin.global.util.SecurityUtil.class)) {
                securityUtilMock.when(com.sofit.admin.global.util.SecurityUtil::getCurrentUserId).thenReturn(1L);
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                // when
                var response = adminAuthService.findMe();

                // then
                assertThat(response.name()).isEqualTo("은행원");
                assertThat(response.loginId()).isEqualTo("bankadmin");
                assertThat(response.role()).isEqualTo(UserRole.ADMIN_BANK_TELLER);
            }
        }

        @Test
        @DisplayName("사용자가 DB에 없으면 USER_NOT_FOUND 예외가 발생한다")
        void findMe_사용자_없으면_USER_NOT_FOUND_예외_발생() {
            // given
            try (var securityUtilMock = org.mockito.Mockito.mockStatic(com.sofit.admin.global.util.SecurityUtil.class)) {
                securityUtilMock.when(com.sofit.admin.global.util.SecurityUtil::getCurrentUserId).thenReturn(999L);
                given(userRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> adminAuthService.findMe())
                        .isInstanceOf(BaseException.class)
                        .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                                .isEqualTo(AdminAuthErrorCode.USER_NOT_FOUND));
            }
        }

        @Test
        @DisplayName("INACTIVE 사용자 조회 시 USER_NOT_FOUND 예외가 발생한다")
        void findMe_INACTIVE_사용자_USER_NOT_FOUND_예외_발생() {
            // given
            User inactiveUser = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            inactiveUser.inactivate();

            try (var securityUtilMock = org.mockito.Mockito.mockStatic(com.sofit.admin.global.util.SecurityUtil.class)) {
                securityUtilMock.when(com.sofit.admin.global.util.SecurityUtil::getCurrentUserId).thenReturn(1L);
                given(userRepository.findById(1L)).willReturn(Optional.of(inactiveUser));

                // when & then
                assertThatThrownBy(() -> adminAuthService.findMe())
                        .isInstanceOf(BaseException.class)
                        .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                                .isEqualTo(AdminAuthErrorCode.USER_NOT_FOUND));
            }
        }
    }

    // ===== 동시 로그인 제한 테스트 =====

    @Nested
    @DisplayName("concurrent login")
    class ConcurrentLoginTest {

        @Test
        @DisplayName("SessionAuthenticationException 발생 시 CONCURRENT_LOGIN 예외가 발생한다")
        void login_동시_세션_초과시_CONCURRENT_LOGIN_예외_발생() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            User bankAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
            given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
            given(loginAttemptService.isBlocked("bankadmin", "127.0.0.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(bankAdmin));
            given(passwordEncoder.matches("password1!", bankAdmin.getPasswordHash())).willReturn(true);
            org.mockito.Mockito.doThrow(new org.springframework.security.web.authentication.session.SessionAuthenticationException("Max sessions exceeded"))
                    .when(sessionAuthenticationStrategy).onAuthentication(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

            // when & then
            assertThatThrownBy(() -> adminAuthService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AdminAuthErrorCode.CONCURRENT_LOGIN));
        }
    }

    // ===== getClientIp 테스트 (X-Forwarded-For blank) =====

    @Nested
    @DisplayName("getClientIp - X-Forwarded-For blank")
    class GetClientIpBlankTest {

        @Test
        @DisplayName("X-Forwarded-For가 blank이면 remoteAddr을 사용한다")
        void login_X_Forwarded_For_blank이면_remoteAddr_사용() {
            // given
            AdminLoginRequest request = createLoginRequest("bankadmin", "password1!");
            User bankAdmin = createAdminUser(1L, "bankadmin", "은행원", UserRole.ADMIN_BANK_TELLER);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(httpRequest.getHeader("X-Forwarded-For")).willReturn("   ");
            given(httpRequest.getRemoteAddr()).willReturn("192.168.1.1");
            given(loginAttemptService.isBlocked("bankadmin", "192.168.1.1")).willReturn(false);
            given(userRepository.findByLoginId("bankadmin")).willReturn(Optional.of(bankAdmin));
            given(passwordEncoder.matches("password1!", bankAdmin.getPasswordHash())).willReturn(true);

            // when
            AdminLoginResponse response = adminAuthService.login(request, httpRequest, httpResponse);

            // then
            assertThat(response).isNotNull();
            verify(loginAttemptService).isBlocked("bankadmin", "192.168.1.1");
        }
    }

    // ===== Helper Methods =====

    private AdminLoginRequest createLoginRequest(String loginId, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private User createActiveUser(Long userId, String loginId, String name) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private User createAdminUser(Long userId, String loginId, String name, UserRole role) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        return user;
    }
}
