package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalItemResponse;
import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.admin.domain.loan.service.ManagerApprovalService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerApprovalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("ManagerApprovalController 단위 테스트")
class ManagerApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @MockitoBean
    private ManagerApprovalService managerApprovalService;

    @Nested
    @DisplayName("GET /api/admin/manager/loan-applications")
    class FindManagerApprovalListTest {

        @Test
        @DisplayName("ADMIN_BANK_MANAGER 권한으로 조회 시 200 응답을 반환한다")
        void shouldReturn200ForBankManager() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);

            ManagerApprovalListResponse response = new ManagerApprovalListResponse(
                    List.of(new ManagerApprovalItemResponse(
                            10L, "2025-06-01", "홍길동", "길동상회",
                            "소상공인 대출", "김은행", 30_000_000L
                    ))
            );
            given(managerApprovalService.findManagerReviewApplications()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/manager/loan-applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.applications[0].applicantName").value("홍길동"));
        }

        @Test
        @DisplayName("ADMIN_DEV 권한으로 조회 시 200 응답을 반환한다")
        void shouldReturn200ForDevAdmin() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);

            ManagerApprovalListResponse response = new ManagerApprovalListResponse(Collections.emptyList());
            given(managerApprovalService.findManagerReviewApplications()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/manager/loan-applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.applications").isEmpty());
        }

        @Test
        @DisplayName("ADMIN_BANK_TELLER 권한으로 조회 시 403 응답을 반환한다")
        void shouldReturn403ForBankTeller() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            mockMvc.perform(get("/api/admin/manager/loan-applications"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4003"));
        }

        @Test
        @DisplayName("USER 권한으로 조회 시 403 응답을 반환한다")
        void shouldReturn403ForUser() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.USER);

            // when & then
            mockMvc.perform(get("/api/admin/manager/loan-applications"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }
}
