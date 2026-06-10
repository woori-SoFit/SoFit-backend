package com.sofit.admin.domain.dev.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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

    @Test
    @DisplayName("배치 수동 트리거 성공 시 200 응답을 반환한다")
    void shouldReturn200OnSuccess() throws Exception {
        // given — jobLauncher.run()이 정상 실행됨 (void 반환이 아닌 경우 mock 기본 반환)
        given(jobLauncher.run(eq(loanDecisionJob), any())).willReturn(null);

        // when & then
        mockMvc.perform(post("/api/admin/dev/batch/loan-decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("loanDecisionJob 실행 완료"));
    }

    @Test
    @DisplayName("배치 실행 실패 시 500 응답을 반환한다")
    void shouldReturn500OnFailure() throws Exception {
        // given
        given(jobLauncher.run(eq(loanDecisionJob), any()))
                .willThrow(new RuntimeException("배치 실행 중 오류 발생"));

        // when & then
        mockMvc.perform(post("/api/admin/dev/batch/loan-decision"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists());
    }
}
