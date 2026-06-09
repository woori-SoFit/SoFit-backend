package com.sofit.user.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.user.service.UserService;
import com.sofit.user.global.filter.SessionValidationFilter;
import com.sofit.user.global.util.SecurityUtil;
import com.sofit.user.global.util.SessionUtil;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController 단위 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private MockedStatic<SessionUtil> sessionUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        sessionUtilMock = mockStatic(SessionUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        sessionUtilMock.close();
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class FindUserTest {

        @Test
        @DisplayName("인증된 사용자 조회 시 200 응답과 프로필 정보를 반환한다")
        void shouldReturnProfileWhenAuthenticated() throws Exception {
            // given
            securityUtilMock.when(SecurityUtil::isAuthenticated).thenReturn(true);
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);

            UserProfileResponse response = new UserProfileResponse(
                    "홍길동", "hong123", "01012345678", "9001011");
            given(userService.findUser(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.name").value("홍길동"))
                    .andExpect(jsonPath("$.result.loginId").value("hong123"))
                    .andExpect(jsonPath("$.result.phoneNumber").value("01012345678"));
        }

        @Test
        @DisplayName("미인증 사용자 조회 시 200 응답을 반환한다")
        void shouldReturnNullResultWhenNotAuthenticated() throws Exception {
            // given
            securityUtilMock.when(SecurityUtil::isAuthenticated).thenReturn(false);

            // when & then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("USER2000"))
                    .andExpect(jsonPath("$.result").doesNotExist());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/me")
    class WithdrawTest {

        @Test
        @DisplayName("회원탈퇴 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
            doNothing().when(userService).withdraw(USER_ID);

            // when & then
            mockMvc.perform(delete("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("USER2002"));
        }
    }
}
