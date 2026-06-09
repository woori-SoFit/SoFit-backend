package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryItemResponse;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.service.DevBatchService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.sGrade.enums.BatchStatus;
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

@WebMvcTest(DevBatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("DevBatchController 단위 테스트")
class DevBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevBatchService devBatchService;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @Nested
    @DisplayName("GET /api/admin/dev/batch/s-grade")
    class FindBatchHistoriesTest {

        @Test
        @DisplayName("ADMIN_DEV 권한으로 배치 이력을 정상 조회한다")
        void shouldReturn200WhenDevAdmin() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);

            BatchHistoryItemResponse item = new BatchHistoryItemResponse(
                    1L, BatchStatus.COMPLETED, 50, 120L, null,
                    LocalDateTime.of(2025, 6, 1, 0, 0),
                    LocalDateTime.of(2025, 6, 1, 0, 2));
            BatchHistoryListResponse response = new BatchHistoryListResponse(
                    List.of(item), 1, 1, 0, 5);
            given(devBatchService.findBatchHistories(0, 5)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/dev/batch/s-grade")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(1))
                    .andExpect(jsonPath("$.result.contents[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.result.contents[0].processedCount").value(50));
        }

        @Test
        @DisplayName("기본 파라미터 없이 조회해도 정상 응답한다")
        void shouldReturn200WithDefaultParams() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);

            BatchHistoryListResponse response = new BatchHistoryListResponse(
                    Collections.emptyList(), 0, 0, 0, 5);
            given(devBatchService.findBatchHistories(null, null)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/dev/batch/s-grade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(0));
        }

        @Test
        @DisplayName("ADMIN_BANK_TELLER 권한이면 403 응답을 반환한다")
        void shouldReturn403WhenBankTeller() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            mockMvc.perform(get("/api/admin/dev/batch/s-grade"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4003"));
        }

        @Test
        @DisplayName("USER 권한이면 403 응답을 반환한다")
        void shouldReturn403WhenUserRole() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.USER);

            // when & then
            mockMvc.perform(get("/api/admin/dev/batch/s-grade"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4003"));
        }

        @Test
        @DisplayName("ADMIN_BANK_MANAGER 권한이면 403 응답을 반환한다")
        void shouldReturn403WhenBankManager() throws Exception {
            // given
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);

            // when & then
            mockMvc.perform(get("/api/admin/dev/batch/s-grade"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4003"));
        }
    }
}
