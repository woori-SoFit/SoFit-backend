package com.sofit.user.domain.loan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.LoanApplicationService;
import com.sofit.user.domain.loan.service.LoanService;
import com.sofit.user.global.filter.SessionValidationFilter;
import com.sofit.user.global.util.SecurityUtil;

@WebMvcTest(LoanApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("LoanApplicationController 단위 테스트")
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private LoanApplicationService loanApplicationService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Nested
    @DisplayName("POST /api/loan-products/{productId}/applications")
    class CreateApplicationTest {

        @Test
        @DisplayName("정상 생성 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationCreateResponse response = new LoanApplicationCreateResponse(100L);
            given(loanApplicationService.createApplication(eq(USER_ID), eq(10L), any()))
                    .willReturn(response);

            String requestBody = """
                    {
                        "annualIncome": "50000000",
                        "creditScore": "750",
                        "incomeType": "SALARY",
                        "existingLoanAmt": "10000000"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/loan-products/10/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.applicationId").value(100));
        }

        @Test
        @DisplayName("중복 신청 시 예외 응답을 반환한다")
        void shouldReturnErrorOnDuplicate() throws Exception {
            // given
            given(loanApplicationService.createApplication(eq(USER_ID), eq(10L), any()))
                    .willThrow(new BaseException(LoanErrorCode.DUPLICATE_APPLICATION));

            String requestBody = """
                    {
                        "annualIncome": "50000000",
                        "creditScore": "750",
                        "incomeType": "SALARY",
                        "existingLoanAmt": "10000000"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/loan-products/10/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/draft")
    class CheckDraftTest {

        @Test
        @DisplayName("DRAFT 존재 시 hasDraft=true를 반환한다")
        void shouldReturnHasDraftTrue() throws Exception {
            // given
            DraftCheckResponse response = new DraftCheckResponse(true, 100L, "STEP_CONSENT", "DRAFT");
            given(loanApplicationService.checkDraft(USER_ID, 10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/draft")
                            .param("productId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.hasDraft").value(true));
        }

        @Test
        @DisplayName("DRAFT 미존재 시 hasDraft=false를 반환한다")
        void shouldReturnHasDraftFalse() throws Exception {
            // given
            DraftCheckResponse response = new DraftCheckResponse(false, null, null, null);
            given(loanApplicationService.checkDraft(USER_ID, 10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/draft")
                            .param("productId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.hasDraft").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/drafts")
    class GetDraftsTest {

        @Test
        @DisplayName("DRAFT 목록 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            DraftListResponse response = new DraftListResponse(Collections.emptyList());
            given(loanApplicationService.findDrafts(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/drafts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/{applicationId}/resume")
    class GetResumeDataTest {

        @Test
        @DisplayName("이어가기 데이터 조회 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationResumeResponse response = new LoanApplicationResumeResponse(
                    100L, "STEP_CONSENT",
                    new LoanApplicationResumeResponse.SavedData("50000000", "750", "SALARY", "10000000", true));
            given(loanApplicationService.getResumeData(USER_ID, 100L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/100/resume"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applicationId").value(100));
        }

        @Test
        @DisplayName("존재하지 않는 신청 조회 시 에러 응답을 반환한다")
        void shouldReturnErrorWhenNotFound() throws Exception {
            // given
            given(loanApplicationService.getResumeData(USER_ID, 999L))
                    .willThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/loan-applications/999/resume"))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/loan-applications/{applicationId}")
    class CancelDraftApplicationTest {

        @Test
        @DisplayName("DRAFT 취소 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            doNothing().when(loanApplicationService).cancelDraftApplication(USER_ID, 100L);

            // when & then
            mockMvc.perform(delete("/api/loan-applications/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/loan-applications/{applicationId}/submit")
    class SubmitApplicationTest {

        @Test
        @DisplayName("최종 제출 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationSubmitResponse response = new LoanApplicationSubmitResponse(
                    100L, "소상공인 대출", 50000000L,
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    "EQUAL_PAYMENT", "WORKING_CAPITAL", 36);
            given(loanApplicationService.submitApplication(eq(USER_ID), eq(100L), any()))
                    .willReturn(response);

            String requestBody = """
                    {
                        "requestedAmount": 50000000,
                        "requestedTerm": 36,
                        "repaymentMethod": "EQUAL_PAYMENT",
                        "purpose": "WORKING_CAPITAL"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/loan-applications/100/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.applicationId").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications")
    class GetUnderReviewLoansTest {

        @Test
        @DisplayName("심사 중 대출 목록 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationListResponse response = LoanApplicationListResponse.builder()
                    .loanApplications(Collections.emptyList()).build();
            given(loanService.findUnderReviewLoans(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/{applicationId}")
    class GetLoanDetailTest {

        @Test
        @DisplayName("심사 중 대출 상세 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationDetailResponse response = LoanApplicationDetailResponse.builder()
                    .applicationId(100L)
                    .productName("소상공인 대출")
                    .status(ApplicationStatus.SUBMITTED)
                    .requestedAmount(50000000L)
                    .requestedTerm(36)
                    .repaymentMethod(RepaymentMethod.EQUAL_PAYMENT)
                    .appliedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .build();
            given(loanService.findLoanDetail(USER_ID, 100L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applicationId").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/completed")
    class GetCompletedLoansTest {

        @Test
        @DisplayName("심사 완료 대출 목록 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            CompletedLoanListResponse response = new CompletedLoanListResponse(Collections.emptyList());
            given(loanService.findCompletedLoans(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/completed"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/loan-applications/completed/{applicationId}")
    class GetCompletedLoanDetailTest {

        @Test
        @DisplayName("심사 완료 대출 상세 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            CompletedLoanDetailResponse response = new CompletedLoanDetailResponse(
                    100L, "소상공인 대출", 50000000L,
                    RepaymentMethod.EQUAL_PAYMENT,
                    new CompletedLoanDetailResponse.DecisionInfo(
                            DecisionStatus.MANAGER_APPROVED, 50000000L,
                            new java.math.BigDecimal("3.5"), 36, "승인합니다."));
            given(loanService.findCompletedLoanDetail(USER_ID, 100L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/loan-applications/completed/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applicationId").value(100));
        }

        @Test
        @DisplayName("존재하지 않는 건 조회 시 에러 응답을 반환한다")
        void shouldReturnErrorWhenNotFound() throws Exception {
            // given
            given(loanService.findCompletedLoanDetail(USER_ID, 999L))
                    .willThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/loan-applications/completed/999"))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }
}
