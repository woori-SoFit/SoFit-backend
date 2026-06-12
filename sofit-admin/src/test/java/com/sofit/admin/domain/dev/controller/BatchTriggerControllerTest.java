package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.service.LoanDecisionBatchService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.entity.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchTriggerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BatchTriggerController 단위 테스트")
class BatchTriggerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private Job loanDecisionJob;

    @MockitoBean
    private LoanDecisionBatchService loanDecisionBatchService;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @Test
    @DisplayName("배치 수동 트리거 성공 시 202 응답을 반환한다")
    void shouldReturn202OnSuccess() throws Exception {
        // given
        given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);
        given(jobLauncher.run(eq(loanDecisionJob), any())).willReturn(null);

        // when & then
        mockMvc.perform(post("/api/admin/dev/batch/loan-decision/trigger"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("BATCH2022"));
    }

    @Test
    @DisplayName("배치 실행 실패 시 BATCH5001 에러 코드를 반환한다")
    void shouldReturnBatch5001OnJobExecutionFailure() throws Exception {
        // given
        given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_DEV);
        given(jobLauncher.run(eq(loanDecisionJob), any()))
                .willThrow(new JobExecutionException("배치 실행 중 오류 발생"));

        // when & then
        mockMvc.perform(post("/api/admin/dev/batch/loan-decision/trigger"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("BATCH5001"));
    }
}
