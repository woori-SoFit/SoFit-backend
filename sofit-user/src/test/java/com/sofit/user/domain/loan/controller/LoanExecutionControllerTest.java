package com.sofit.user.domain.loan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.LoanExecutionService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(LoanExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LoanExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanExecutionService loanExecutionService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    @BeforeEach
    void setUpSecurityContext() {
        var authentication = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("대출 실행 결과 조회 시 200 + LOAN2010 응답을 반환한다")
    void getExecutionResult_returns200() throws Exception {
        // given
        LoanExecutionResultResponse response = new LoanExecutionResultResponse(
                1L, APPLICATION_ID, 10L, "우리 사업자 대출",
                9_000_000L, new java.math.BigDecimal("5.50"), 12, RepaymentMethod.EQUAL_PAYMENT);
        given(loanExecutionService.findExecutionResult(eq(USER_ID), eq(APPLICATION_ID)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/loan-applications/{id}/execution", APPLICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2010"))
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID));
    }

    @Test
    @DisplayName("계좌 인증 요청 시 200 + ACCOUNT2011 응답을 반환한다")
    void requestAccountVerification_returns200() throws Exception {
        // given
        AccountVerificationResponse response =
                new AccountVerificationResponse("1234-****-90", "SOFIT213", "2026-06-01T10:05:00");
        given(loanExecutionService.requestAccountVerification(eq(USER_ID), eq(APPLICATION_ID), any()))
                .willReturn(response);

        String body = """
                {"accountNumber": "1234567890"}
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{id}/account-verification", APPLICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACCOUNT2011"))
                .andExpect(jsonPath("$.result.maskedAccountNumber").value("1234-****-90"));
    }

    @Test
    @DisplayName("계좌번호 형식이 잘못되면 검증 실패로 400을 반환한다")
    void requestAccountVerification_invalidAccount_returns400() throws Exception {
        // given
        String body = """
                {"accountNumber": "abc"}
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{id}/account-verification", APPLICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("계좌 인증 확인 시 200 + ACCOUNT2012 응답을 반환한다")
    void confirmAccountVerification_returns200() throws Exception {
        // given
        given(loanExecutionService.confirmAccountVerification(eq(USER_ID), eq(APPLICATION_ID), any()))
                .willReturn(new AccountVerificationConfirmResponse(true));

        String body = """
                {"verificationCode": "213"}
                """;
;
        // when & then
        mockMvc.perform(post("/api/loan-applications/{id}/account-verification/confirm", APPLICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACCOUNT2012"))
                .andExpect(jsonPath("$.result.accountVerified").value(true));
    }

    @Test
    @DisplayName("실행 건이 없으면 404 + LOAN4044 응답을 반환한다")
    void getExecutionResult_notFound_returns404() throws Exception {
        // given
        given(loanExecutionService.findExecutionResult(eq(USER_ID), eq(APPLICATION_ID)))
                .willThrow(new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/loan-applications/{id}/execution", APPLICATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4044"));
    }

    @Test
    @DisplayName("본인 소유가 아닌 신청 건에 계좌 인증 요청 시 403 + LOAN4032 응답을 반환한다")
    void requestAccountVerification_notOwned_returns403() throws Exception {
        // given
        given(loanExecutionService.requestAccountVerification(eq(USER_ID), eq(APPLICATION_ID), any()))
                .willThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED));

        String body = """
                {"accountNumber": "1234567890"}
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{id}/account-verification", APPLICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4032"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 실행 결과 조회 시 401 + COMMON4001 응답을 반환한다")
    void getExecutionResult_unauthenticated_returns401() throws Exception {
        // given: 인증 컨텍스트 제거 → SecurityUtil.getCurrentUserId()가 UNAUTHORIZED 발생
        SecurityContextHolder.clearContext();

        // when & then
        mockMvc.perform(get("/api/loan-applications/{id}/execution", APPLICATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(GeneralErrorCode.UNAUTHORIZED.getCode()));
    }
}
