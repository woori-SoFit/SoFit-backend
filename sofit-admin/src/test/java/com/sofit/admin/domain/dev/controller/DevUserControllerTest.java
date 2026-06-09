package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.UserItemResponse;
import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.admin.domain.dev.dto.response.UserStatisticsResponse;
import com.sofit.admin.domain.dev.service.DevUserService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("DevUserController 단위 테스트")
class DevUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevUserService devUserService;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @Nested
    @DisplayName("GET /api/admin/users")
    class FindUsersTest {

        @Test
        @DisplayName("기본 파라미터로 사용자 목록을 조회한다")
        void shouldReturn200WithDefaultParams() throws Exception {
            // given
            UserListResponse response = new UserListResponse(
                    Collections.emptyList(), 0, 0, 0, 8);
            given(devUserService.findUsers(null, null, null, null, null))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(0));
        }

        @Test
        @DisplayName("페이징 및 필터 파라미터로 사용자 목록을 조회한다")
        void shouldReturn200WithFilterParams() throws Exception {
            // given
            UserItemResponse item = new UserItemResponse(
                    1L, "hong123", "홍길동", "USER", "ACTIVE",
                    "01012345678", LocalDateTime.of(2025, 1, 1, 0, 0));
            UserListResponse response = new UserListResponse(
                    List.of(item), 1, 1, 0, 8);
            given(devUserService.findUsers(0, 8, "홍", "USER", "ACTIVE"))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/users")
                            .param("page", "0")
                            .param("size", "8")
                            .param("keyword", "홍")
                            .param("role", "USER")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalCount").value(1))
                    .andExpect(jsonPath("$.result.contents[0].name").value("홍길동"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/statistics")
    class FindUserStatisticsTest {

        @Test
        @DisplayName("ADMIN_DEV 권한으로 통계를 정상 조회한다")
        void shouldReturn200WhenDevAdmin() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);
            UserStatisticsResponse response = new UserStatisticsResponse(100, 80, 10, 70, 20);
            given(devUserService.findUserStatistics()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalCount").value(100))
                    .andExpect(jsonPath("$.result.activeCount").value(80))
                    .andExpect(jsonPath("$.result.bankerCount").value(10))
                    .andExpect(jsonPath("$.result.userCount").value(70))
                    .andExpect(jsonPath("$.result.inactiveCount").value(20));
        }

        @Test
        @DisplayName("ADMIN_BANK_TELLER 권한으로 통계를 정상 조회한다")
        void shouldReturn200WhenBankTeller() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            UserStatisticsResponse response = new UserStatisticsResponse(50, 40, 5, 35, 10);
            given(devUserService.findUserStatistics()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("ADMIN_BANK_MANAGER 권한으로 통계를 정상 조회한다")
        void shouldReturn200WhenBankManager() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);
            UserStatisticsResponse response = new UserStatisticsResponse(50, 40, 5, 35, 10);
            given(devUserService.findUserStatistics()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("USER 권한이면 403 응답을 반환한다")
        void shouldReturn403WhenUserRole() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.USER);

            // when & then
            mockMvc.perform(get("/api/admin/users/statistics"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4003"));
        }
    }
}
