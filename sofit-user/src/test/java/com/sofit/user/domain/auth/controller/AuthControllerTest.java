package com.sofit.user.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.CheckLoginIdResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.auth.service.AuthService;
import com.sofit.user.global.filter.SessionValidationFilter;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    @Nested
    @DisplayName("POST /api/auth/signup/business-verification")
    class VerifyBusinessTest {

        @Test
        @DisplayName("사업자등록번호 인증 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            BusinessVerificationResponse response = new BusinessVerificationResponse(
                    "1234567890", "홍길동", "길동상회", "음식점업",
                    LocalDate.of(2020, 1, 15), LocalDateTime.now());
            given(authService.verifyBusiness(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/auth/signup/business-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "businessNumber", "1234567890"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.businessNumber").value("1234567890"))
                    .andExpect(jsonPath("$.result.representativeName").value("홍길동"));
        }

        @Test
        @DisplayName("사업자등록번호 형식 오류 시 400 응답을 반환한다")
        void shouldReturn400WhenInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/auth/signup/business-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "businessNumber", "123-45-67890"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("사업자등록번호 미입력 시 400 응답을 반환한다")
        void shouldReturn400WhenEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/signup/business-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "businessNumber", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signup/verify-pin")
    class VerifyPinTest {

        @Test
        @DisplayName("PIN 인증 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            doNothing().when(authService).verifyFinancialCertificate(any(), any());

            // when & then
            mockMvc.perform(post("/api/auth/signup/verify-pin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "01012345678",
                                    "holderName", "홍길동",
                                    "residentNumber", "9001011",
                                    "pin", "123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("PIN 형식 오류 시 400 응답을 반환한다")
        void shouldReturn400WhenInvalidPin() throws Exception {
            mockMvc.perform(post("/api/auth/signup/verify-pin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "01012345678",
                                    "holderName", "홍길동",
                                    "residentNumber", "9001011",
                                    "pin", "12345"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signup/complete")
    class CompleteSignupTest {

        @Test
        @DisplayName("회원가입 완료 시 201 응답을 반환한다")
        void shouldReturn201OnSuccess() throws Exception {
            // given
            SignupCompleteResponse response = new SignupCompleteResponse(1L, "hong123", "홍길동", "USER");
            given(authService.completeSignup(any(), any())).willReturn(response);

            Map<String, Object> request = Map.of(
                    "loginId", "hong123",
                    "password", "Pass1234!",
                    "name", "홍길동",
                    "residentNumber", "9001011",
                    "phoneNumber", "01012345678",
                    "consents", List.of(Map.of("termId", 1, "isConsented", true)));

            // when & then
            mockMvc.perform(post("/api/auth/signup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.loginId").value("hong123"));
        }

        @Test
        @DisplayName("아이디 형식 오류 시 400 응답을 반환한다")
        void shouldReturn400WhenInvalidLoginId() throws Exception {
            Map<String, Object> request = Map.of(
                    "loginId", "ab",
                    "password", "Pass1234!",
                    "name", "홍길동",
                    "residentNumber", "9001011",
                    "phoneNumber", "01012345678",
                    "consents", List.of(Map.of("termId", 1, "isConsented", true)));

            mockMvc.perform(post("/api/auth/signup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoginResponse response = new LoginResponse(1L, "홍길동", "USER");
            given(authService.login(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "hong123",
                                    "password", "Pass1234!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.userId").value(1))
                    .andExpect(jsonPath("$.result.name").value("홍길동"));
        }

        @Test
        @DisplayName("loginId 미입력 시 400 응답을 반환한다")
        void shouldReturn400WhenLoginIdBlank() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "",
                                    "password", "Pass1234!"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("로그인 실패 시 401 응답을 반환한다")
        void shouldReturn401WhenLoginFailed() throws Exception {
            // given
            given(authService.login(any(), any(), any()))
                    .willThrow(new BaseException(AuthErrorCode.LOGIN_FAILED));

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", "hong123",
                                    "password", "wrongpass1!"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/signup/check-login-id")
    class CheckLoginIdTest {

        @Test
        @DisplayName("사용 가능한 아이디 확인 시 200 응답을 반환한다")
        void shouldReturn200WhenAvailable() throws Exception {
            // given
            given(authService.checkLoginId("newhong"))
                    .willReturn(new CheckLoginIdResponse("newhong", true));

            // when & then
            mockMvc.perform(get("/api/auth/signup/check-login-id")
                            .param("loginId", "newhong"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.loginId").value("newhong"))
                    .andExpect(jsonPath("$.result.available").value(true));
        }

        @Test
        @DisplayName("이미 사용 중인 아이디 확인 시 available=false를 반환한다")
        void shouldReturnFalseWhenTaken() throws Exception {
            // given
            given(authService.checkLoginId("existinguser"))
                    .willReturn(new CheckLoginIdResponse("existinguser", false));

            // when & then
            mockMvc.perform(get("/api/auth/signup/check-login-id")
                            .param("loginId", "existinguser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.available").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }
}
