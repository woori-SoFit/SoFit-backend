package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionErrorCode;
import com.sofit.admin.domain.loan.service.LoanDecisionService;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.loan.enums.DecisionStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanDecisionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("LoanDecisionController 단위 테스트")
class LoanDecisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanDecisionService loanDecisionService;

    @Nested
    @DisplayName("POST /api/admin/loan-applications/{applicationId}/approve")
    class ApproveLoanApplicationTest {

        @Test
        @DisplayName("정상 승인 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanDecisionResponse response = new LoanDecisionResponse(1L, 10L, DecisionStatus.TELLER_APPROVED);
            given(loanDecisionService.approveLoanApplication(eq(10L), any())).willReturn(response);

            String requestBody = """
                    {
                        "approvedAmount": 50000000,
                        "approvedRate": 3.5,
                        "approvedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "comment": "승인합니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admin/loan-applications/10/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.decision").value("TELLER_APPROVED"))
                    .andExpect(jsonPath("$.result.applicationId").value(10));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
        void shouldReturn400WhenMissingRequiredFields() throws Exception {
            String requestBody = """
                    {
                        "approvedAmount": null,
                        "approvedRate": 3.5,
                        "approvedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "comment": "승인합니다."
                    }
                    """;

            mockMvc.perform(post("/api/admin/loan-applications/10/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("comment가 빈 문자열이면 400 응답을 반환한다")
        void shouldReturn400WhenCommentIsBlank() throws Exception {
            String requestBody = """
                    {
                        "approvedAmount": 50000000,
                        "approvedRate": 3.5,
                        "approvedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "comment": ""
                    }
                    """;

            mockMvc.perform(post("/api/admin/loan-applications/10/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("이미 결정된 건이면 409 응답을 반환한다")
        void shouldReturn409WhenAlreadyDecided() throws Exception {
            // given
            given(loanDecisionService.approveLoanApplication(eq(10L), any()))
                    .willThrow(new BaseException(LoanDecisionErrorCode.ALREADY_DECIDED));

            String requestBody = """
                    {
                        "approvedAmount": 50000000,
                        "approvedRate": 3.5,
                        "approvedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "comment": "승인합니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admin/loan-applications/10/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("LOAN_ADMIN4091"));
        }

        @Test
        @DisplayName("존재하지 않는 건이면 404 응답을 반환한다")
        void shouldReturn404WhenNotFound() throws Exception {
            // given
            given(loanDecisionService.approveLoanApplication(eq(999L), any()))
                    .willThrow(new BaseException(LoanDecisionErrorCode.APPLICATION_NOT_FOUND));

            String requestBody = """
                    {
                        "approvedAmount": 50000000,
                        "approvedRate": 3.5,
                        "approvedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "comment": "승인합니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admin/loan-applications/999/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LOAN_ADMIN4041"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/loan-applications/{applicationId}/reject")
    class RejectLoanApplicationTest {

        @Test
        @DisplayName("정상 거절 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanDecisionResponse response = new LoanDecisionResponse(2L, 10L, DecisionStatus.TELLER_REJECTED);
            given(loanDecisionService.rejectLoanApplication(eq(10L), any())).willReturn(response);

            String requestBody = """
                    {
                        "comment": "신용도 부족으로 거절합니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admin/loan-applications/10/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.decision").value("TELLER_REJECTED"));
        }

        @Test
        @DisplayName("comment가 빈 문자열이면 400 응답을 반환한다")
        void shouldReturn400WhenCommentIsBlank() throws Exception {
            String requestBody = """
                    {
                        "comment": ""
                    }
                    """;

            mockMvc.perform(post("/api/admin/loan-applications/10/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("권한이 없으면 403 응답을 반환한다")
        void shouldReturn403WhenNoAuthority() throws Exception {
            // given
            given(loanDecisionService.rejectLoanApplication(eq(10L), any()))
                    .willThrow(new BaseException(LoanDecisionErrorCode.NO_DECISION_AUTHORITY));

            String requestBody = """
                    {
                        "comment": "거절합니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admin/loan-applications/10/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("LOAN_ADMIN4031"));
        }
    }
}
