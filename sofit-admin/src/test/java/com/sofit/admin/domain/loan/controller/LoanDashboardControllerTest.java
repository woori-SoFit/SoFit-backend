package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.*;
import com.sofit.admin.domain.loan.service.*;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("LoanDashboardController 단위 테스트")
class LoanDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanDashboardService loanDashboardService;

    @MockitoBean
    private LoanApplicationInfoService loanApplicationInfoService;

    @MockitoBean
    private MyBizDataDetailService myBizDataDetailService;

    @MockitoBean
    private LoanApplicationGradeService loanApplicationGradeService;

    @MockitoBean
    private LoanStatisticsService loanStatisticsService;

    @MockitoBean
    private LoanApplicationReviewService loanApplicationReviewService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications")
    class FindLoanApplicationsTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanDashboardResponse response = new LoanDashboardResponse(
                    0, 0, 0, 10, Collections.emptyList());
            given(loanDashboardService.findLoanApplications(any(), eq(false), eq(1L), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(0));
        }

        @Test
        @DisplayName("page가 음수이면 400 응답을 반환한다")
        void shouldReturn400WhenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "-1")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4000"));
        }

        @Test
        @DisplayName("size가 0이면 400 응답을 반환한다")
        void shouldReturn400WhenSizeIsZero() throws Exception {
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("size가 101이면 400 응답을 반환한다")
        void shouldReturn400WhenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("유효하지 않은 status 값이면 400 응답을 반환한다")
        void shouldReturn400WhenInvalidStatus() throws Exception {
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("LOAN4001"));
        }

        @Test
        @DisplayName("허용되지 않은 status 값(SUBMITTED)이면 400 응답을 반환한다")
        void shouldReturn400WhenNotAllowedStatus() throws Exception {
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("status", "SUBMITTED"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("LOAN4001"));
        }

        @Test
        @DisplayName("유효한 status 필터로 조회 시 200 응답을 반환한다")
        void shouldReturn200WithValidStatusFilter() throws Exception {
            // given
            LoanDashboardResponse response = new LoanDashboardResponse(
                    1, 1, 0, 10, List.of(
                    new LoanApplicationItemResponse(
                            10L, LocalDateTime.of(2025, 6, 1, 10, 0),
                            "홍길동", "길동상회", "소상공인 대출",
                            ApplicationStatus.SYSTEM_APPROVED, 50L, "김은행", 10000000L, null
                    )
            ));
            given(loanDashboardService.findLoanApplications(any(), eq(false), eq(1L), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("status", "SYSTEM_APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalCount").value(1))
                    .andExpect(jsonPath("$.result.contents[0].applicantName").value("홍길동"));
        }

        @Test
        @DisplayName("status에 blank 값이 포함되면 무시하고 정상 조회한다")
        void shouldIgnoreBlankStatusValues() throws Exception {
            // given
            LoanDashboardResponse response = new LoanDashboardResponse(
                    0, 0, 0, 10, Collections.emptyList());
            given(loanDashboardService.findLoanApplications(any(), eq(false), eq(1L), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("status", "")
                            .param("status", "SYSTEM_APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("status가 모두 blank이면 전체 조회한다")
        void shouldQueryAllWhenAllStatusesAreBlank() throws Exception {
            // given
            LoanDashboardResponse response = new LoanDashboardResponse(
                    0, 0, 0, 10, Collections.emptyList());
            given(loanDashboardService.findLoanApplications(any(), eq(false), eq(1L), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("status", "")
                            .param("status", "  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/{applicationId}")
    class FindLoanApplicationDetailTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationDetailResponse response = new LoanApplicationDetailResponse(
                    10L, "홍길동", "길동상회", "소상공인 대출",
                    "SYSTEM_APPROVED", "2025-06-01 10:00:00", 50L, "김은행");
            given(loanDashboardService.findLoanApplicationDetail(10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applicationId").value(10))
                    .andExpect(jsonPath("$.result.applicantName").value("홍길동"));
        }

        @Test
        @DisplayName("존재하지 않는 건 조회 시 404 응답을 반환한다")
        void shouldReturn404WhenNotFound() throws Exception {
            // given
            given(loanDashboardService.findLoanApplicationDetail(999L))
                    .willThrow(new BaseException(GeneralErrorCode.NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4004"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/{applicationId}/info")
    class FindLoanApplicationInfoTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationInfoResponse response = new LoanApplicationInfoResponse(
                    new LoanApplicationInfoResponse.ApplicantInfo("홍길동", "9001011", "01012345678",
                            LocalDateTime.of(2025, 1, 1, 0, 0), "hong123"),
                    new LoanApplicationInfoResponse.BusinessInfo("길동상회", "1234567890", "음식점업",
                            "한식", "서울시 강남구", null),
                    new LoanApplicationInfoResponse.ApplicationInfo(50_000_000L, 36, "WORKING_CAPITAL", "EQUAL_PAYMENT"),
                    new LoanApplicationInfoResponse.UserInputInfo(null, null, null, null),
                    Collections.emptyList()
            );
            given(loanApplicationInfoService.findLoanApplicationInfo(10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/10/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applicantInfo.name").value("홍길동"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/{applicationId}/mybiz-data")
    class FindMyBizDataDetailTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            MyBizDataDetailResponse response = new MyBizDataDetailResponse(
                    1, 130_000_000L, 30_400_000L, 2_530_000L, 15_000_000L,
                    18, "FILED", "2026-04-25", false, "PAID",
                    List.of(new MyBizDataDetailResponse.RevenueTrendItem("2026-05", 11_500_000L)),
                    List.of(new MyBizDataDetailResponse.ProfitTrendItem("2026-05", 3_100_000L)),
                    List.of(new MyBizDataDetailResponse.IndustryAvgRevenueTrendItem("2026-05", 9_200_000L)),
                    new MyBizDataDetailResponse.IndustryComparisonResponse(
                            11_500_000L, 9_200_000L, 9_800_000L,
                            new java.math.BigDecimal("27.00"), new java.math.BigDecimal("19.80"), new java.math.BigDecimal("21.30"),
                            new java.math.BigDecimal("8.20"), new java.math.BigDecimal("12.50"), new java.math.BigDecimal("15.30"),
                            new java.math.BigDecimal("6.80"), new java.math.BigDecimal("10.20"), new java.math.BigDecimal("11.70")));
            given(myBizDataDetailService.findMyBizDataDetail(10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/10/mybiz-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.annualIncome").value(130_000_000))
                    .andExpect(jsonPath("$.result.existingLoanCount").value(1))
                    .andExpect(jsonPath("$.result.industryComparison.myRevenue").value(11_500_000));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/statistics")
    class GetStatisticsTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanStatisticsResponse response = new LoanStatisticsResponse(8, 2, 10, 4);
            given(loanStatisticsService.getStatistics()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.pending").value(8))
                    .andExpect(jsonPath("$.result.managerReview").value(2))
                    .andExpect(jsonPath("$.result.approved").value(10))
                    .andExpect(jsonPath("$.result.rejected").value(4));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/{applicationId}/review")
    class FindLoanApplicationReviewTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationReviewResponse response = new LoanApplicationReviewResponse(
                    new LoanApplicationReviewResponse.ProductInfoResponse(
                            "소상공인 대출", 1_000_000L, 100_000_000L,
                            new BigDecimal("2.5"), new BigDecimal("8.0"),
                            12, 60, List.of("EQUAL_PAYMENT"), List.of("WORKING_CAPITAL")),
                    new LoanApplicationReviewResponse.ApplicationInfoResponse(
                            50_000_000L, 36, "WORKING_CAPITAL", "EQUAL_PAYMENT"),
                    null,
                    Collections.emptyList()
            );
            given(loanApplicationReviewService.findLoanApplicationReview(10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/10/review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.productInfo.productName").value("소상공인 대출"))
                    .andExpect(jsonPath("$.result.applicationInfo.requestedAmount").value(50_000_000));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/loan-applications/{applicationId}/grade")
    class FindLoanApplicationGradeTest {

        @Test
        @DisplayName("정상 조회 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            LoanApplicationGradeResponse response = new LoanApplicationGradeResponse(
                    new LoanApplicationGradeResponse.CbScoreInfo(750, 1000),
                    "S3",
                    new LoanApplicationGradeResponse.ScbInfo(800, 1000, 50),
                    new LoanApplicationGradeResponse.ShapResult(
                            "S3", "S2",
                            List.of("매출 성장"), List.of("업종 순위"),
                            java.util.Map.of("매출 성장", 0.35),
                            java.util.Map.of("업종 순위", -0.15),
                            "매출 성장세를 유지하세요.")
            );
            given(loanApplicationGradeService.findLoanApplicationGrade(10L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/10/grade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.cbScore.score").value(750))
                    .andExpect(jsonPath("$.result.sGrade").value("S3"))
                    .andExpect(jsonPath("$.result.scbInfo.score").value(800))
                    .andExpect(jsonPath("$.result.scbInfo.bonusPoints").value(50))
                    .andExpect(jsonPath("$.result.shapResult.grade").value("S3"))
                    .andExpect(jsonPath("$.result.shapResult.targetGrade").value("S2"))
                    .andExpect(jsonPath("$.result.shapResult.advice").value("매출 성장세를 유지하세요."));
        }

        @Test
        @DisplayName("존재하지 않는 건 조회 시 404 응답을 반환한다")
        void shouldReturn404WhenNotFound() throws Exception {
            // given
            given(loanApplicationGradeService.findLoanApplicationGrade(999L))
                    .willThrow(new BaseException(GeneralErrorCode.NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/admin/loan-applications/999/grade"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON4004"));
        }
    }
}
