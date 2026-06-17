package com.sofit.admin.domain.auth;

import com.sofit.admin.domain.auth.controller.AdminAuthController;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.admin.domain.auth.service.AdminAuthService;
import com.sofit.admin.domain.auth.service.LoginAttemptService;
import com.sofit.admin.global.config.CustomAccessDeniedHandler;
import com.sofit.admin.global.config.CustomAuthenticationEntryPoint;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 중복 로그인 방지 및 로그인 횟수 제한 테스트.
 * DB/Redis 연결 없이 Mock 기반으로 동작한다.
 */
@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminAuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    // SecurityConfig 관련 Bean Mock (WebMvcTest에서 필요할 수 있음)
    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private static final String LOGIN_URL = "/api/admin/auth/login";
    private static final String VALID_LOGIN_ID = "dev_admin";
    private static final String VALID_PASSWORD = "sofit1234!";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @Nested
    @DisplayName("로그인 횟수 제한 (브루트포스 방어)")
    class LoginAttemptLimitTest {

        @Test
        @DisplayName("계정이 잠긴 상태에서 로그인 시도 시 429 응답")
        void shouldReturn429WhenAccountIsLocked() throws Exception {
            // given: 계정이 잠긴 상태
            given(loginAttemptService.isBlocked(VALID_LOGIN_ID)).willReturn(true);

            // AdminAuthService에서 ACCOUNT_LOCKED 예외 발생하도록 설정
            given(adminAuthService.login(any(), any(), any()))
                    .willThrow(new BaseException(AdminAuthErrorCode.ACCOUNT_LOCKED));

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH4291"))
                    .andExpect(jsonPath("$.message").value("로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해 주세요."));
        }

        @Test
        @DisplayName("계정이 잠기지 않은 상태에서 로그인 실패 시 400 응답")
        void shouldReturn400WhenLoginFailsAndNotLocked() throws Exception {
            // given: 계정 잠기지 않음
            given(loginAttemptService.isBlocked(VALID_LOGIN_ID)).willReturn(false);

            // AdminAuthService에서 LOGIN_FAILED 예외 발생
            given(adminAuthService.login(any(), any(), any()))
                    .willThrow(new BaseException(AdminAuthErrorCode.LOGIN_FAILED));

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, WRONG_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH4001"));
        }

        @Test
        @DisplayName("로그인 성공 시 200 응답")
        void shouldReturn200OnSuccessfulLogin() throws Exception {
            // given
            given(loginAttemptService.isBlocked(VALID_LOGIN_ID)).willReturn(false);

            AdminLoginResponse loginResponse = new AdminLoginResponse(
                    1L, "개발자관리자", "ADMIN_DEV"
            );
            given(adminAuthService.login(any(), any(), any())).willReturn(loginResponse);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.name").value("개발자관리자"));
        }
    }

    @Nested
    @DisplayName("중복 로그인 방지 (동시 세션 제한)")
    class ConcurrentLoginTest {

        @Test
        @DisplayName("동시 로그인 시도 시 409 응답")
        void shouldReturn409WhenConcurrentLogin() throws Exception {
            // given: 이미 세션이 존재하여 동시 로그인 차단
            given(loginAttemptService.isBlocked(VALID_LOGIN_ID)).willReturn(false);
            given(adminAuthService.login(any(), any(), any()))
                    .willThrow(new BaseException(AdminAuthErrorCode.CONCURRENT_LOGIN));

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH4091"))
                    .andExpect(jsonPath("$.message").value("이미 다른 기기에서 로그인되어 있습니다."));
        }
    }

    private String loginJson(String loginId, String password) {
        return String.format("{\"loginId\":\"%s\",\"password\":\"%s\"}", loginId, password);
    }
}
