package com.sofit.user.domain.loanApplication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.user.domain.loan.controller.LoanApplicationController;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.LoanApplicationService;
import com.sofit.user.domain.loan.service.LoanService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(LoanApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanApplicationService loanApplicationService;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    @BeforeEach
    void setUpSecurityContext() {
        // SecurityUtil.getCurrentUserId()가 SecurityContextHolder에서 userId를 읽으므로
        // 테스트마다 Authentication을 직접 주입한다
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // === POST /api/loan-products/{productId}/applications ===

    @Test
    @DisplayName("대출 신청 생성 - 200 + LOAN2003 반환")
    void createApplication_returns200() throws Exception {
        // given
        LoanApplicationCreateResponse response = new LoanApplicationCreateResponse(APPLICATION_ID);
        given(loanApplicationService.createApplication(anyLong(), anyLong(), any()))
                .willReturn(response);

        String requestBody = """
                {
                    "annualIncome": "AMT_30_50M",
                    "creditScore": "CS_0_850",
                    "incomeType": "BUSINESS",
                    "existingLoanAmt": "LOAN_NONE"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-products/{productId}/applications", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2003"))
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID));
    }

    @Test
    @DisplayName("대출 신청 생성 - 중복 신청이면 409 반환")
    void createApplication_returns409_whenDuplicate() throws Exception {
        // given
        given(loanApplicationService.createApplication(anyLong(), anyLong(), any()))
                .willThrow(new BaseException(LoanErrorCode.DUPLICATE_APPLICATION));

        String requestBody = """
                {
                    "annualIncome": "AMT_30_50M",
                    "creditScore": "CS_0_850",
                    "incomeType": "BUSINESS",
                    "existingLoanAmt": "LOAN_NONE"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-products/{productId}/applications", PRODUCT_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4091"));
    }

    // === GET /api/loan-applications/draft?productId={productId} ===

    @Test
    @DisplayName("DRAFT 존재 여부 확인 - DRAFT 있으면 200 + hasDraft=true 반환")
    void checkDraft_returns200_withDraft() throws Exception {
        // given
        DraftCheckResponse response = new DraftCheckResponse(true, APPLICATION_ID, "CONSENT_DONE", "BIZ_INFO");
        given(loanApplicationService.checkDraft(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-applications/draft")
                        .sessionAttr("userId", USER_ID)
                        .param("productId", String.valueOf(PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2004"))
                .andExpect(jsonPath("$.result.hasDraft").value(true))
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.resumeStep").value("BIZ_INFO"));
    }

    @Test
    @DisplayName("DRAFT 존재 여부 확인 - DRAFT 없으면 200 + hasDraft=false 반환")
    void checkDraft_returns200_withoutDraft() throws Exception {
        // given
        DraftCheckResponse response = new DraftCheckResponse(false, null, null, null);
        given(loanApplicationService.checkDraft(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-applications/draft")
                        .sessionAttr("userId", USER_ID)
                        .param("productId", String.valueOf(PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasDraft").value(false));
    }

    // === GET /api/loan-applications/{applicationId}/resume ===

    @Test
    @DisplayName("이어가기 데이터 조회 - 200 + LOAN2011 반환")
    void getResumeData_returns200() throws Exception {
        // given
        LoanApplicationResumeResponse.SavedData savedData = new LoanApplicationResumeResponse.SavedData(
                "AMT_30_50M", "CS_0_850", "01", "LOAN_NONE", false);
        LoanApplicationResumeResponse response = new LoanApplicationResumeResponse(
                APPLICATION_ID, "CONSENT", savedData);

        given(loanApplicationService.getResumeData(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-applications/{applicationId}/resume", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2011"))
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.resumeStep").value("CONSENT"));
    }

    @Test
    @DisplayName("이어가기 데이터 조회 - 신청 미존재이면 404 반환")
    void getResumeData_returns404_whenNotFound() throws Exception {
        // given
        given(loanApplicationService.getResumeData(anyLong(), anyLong()))
                .willThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/loan-applications/{applicationId}/resume", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4042"));
    }

    // === POST /api/loan-applications/{applicationId}/submit ===

    @Test
    @DisplayName("대출 최종 제출 - 200 + LOAN2005 반환")
    void submitApplication_returns200() throws Exception {
        // given
        LoanApplicationSubmitResponse response = new LoanApplicationSubmitResponse(
                APPLICATION_ID, "소상공인 성장 대출", 30_000_000L,
                LocalDateTime.now(), "EQUAL_PAYMENT", "WORKING_CAPITAL", 36);

        given(loanApplicationService.submitApplication(anyLong(), anyLong(), any()))
                .willReturn(response);

        String requestBody = """
                {
                    "requestedAmount": 30000000,
                    "requestedTerm": 36,
                    "repaymentMethod": "EQUAL_PAYMENT",
                    "purpose": "WORKING_CAPITAL"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/submit", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2005"))
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.requestedAmount").value(30_000_000));
    }

    // === GET /api/loan-applications ===

    @Test
    @DisplayName("심사 중인 대출 목록 조회 - 200 + LOAN2006 반환")
    void getUnderReviewLoans_returns200() throws Exception {
        // given
        LoanApplicationListResponse response = LoanApplicationListResponse.builder()
                .loanApplications(List.of(
                        LoanApplicationListResponse.LoanApplicationItem.builder()
                                .applicationId(APPLICATION_ID)
                                .productName("소상공인 성장 대출")
                                .status(ApplicationStatus.SUBMITTED)
                                .requestedAmount(30_000_000L)
                                .build()
                ))
                .build();

        given(loanService.findUnderReviewLoans(anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-applications")
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2006"))
                .andExpect(jsonPath("$.result.loanApplications").isArray())
                .andExpect(jsonPath("$.result.loanApplications[0].applicationId").value(APPLICATION_ID));
    }
}
