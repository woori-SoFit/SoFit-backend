package com.sofit.admin.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.admin.domain.auth.service.AdminAuthService;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminAuthController 단위 테스트")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AdminAuthService adminAuthService;

    @Nested
    @DisplayName("POST /api/admin/auth/login")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            AdminLoginResponse response = new AdminLoginResponse(1L, "은행원", "ADMIN_BANK_TELLER");
            given(adminAuthService.login(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "bankadmin",
                                    "password", "password1!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.name").value("은행원"))
                    .andExpect(jsonPath("$.result.role").value("ADMIN_BANK_TELLER"));
        }

        @Test
        @DisplayName("loginId 미입력 시 400 응답을 반환한다")
        void shouldReturn400WhenLoginIdBlank() throws Exception {
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "",
                                    "password", "password1!"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("password 미입력 시 400 응답을 반환한다")
        void shouldReturn400WhenPasswordBlank() throws Exception {
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "bankadmin",
                                    "password", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("계정 잠금 시 429 응답을 반환한다")
        void shouldReturn429WhenAccountLocked() throws Exception {
            // given
            given(adminAuthService.login(any(), any(), any()))
                    .willThrow(new BaseException(AdminAuthErrorCode.ACCOUNT_LOCKED));

            // when & then
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "bankadmin",
                                    "password", "wrongpass"))))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/auth/me")
    class FindMeTest {

        @Test
        @DisplayName("현재 로그인한 관리자 정보를 조회한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            AdminMeResponse response = new AdminMeResponse(
                    1L, "은행원", "bankadmin", "01012345678", UserRole.ADMIN_BANK_TELLER);
            given(adminAuthService.findMe()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.name").value("은행원"))
                    .andExpect(jsonPath("$.result.role").value("ADMIN_BANK_TELLER"));
        }

        @Test
        @DisplayName("세션 만료 시 401 응답을 반환한다")
        void shouldReturn401WhenSessionExpired() throws Exception {
            // given
            given(adminAuthService.findMe())
                    .willThrow(new BaseException(AdminAuthErrorCode.SESSION_EXPIRED));

            // when & then
            mockMvc.perform(get("/api/admin/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/auth/logout")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            doNothing().when(adminAuthService).logout(any(), any());

            // when & then
            mockMvc.perform(post("/api/admin/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }
}
