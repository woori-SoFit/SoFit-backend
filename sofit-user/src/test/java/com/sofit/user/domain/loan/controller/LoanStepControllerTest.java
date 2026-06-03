package com.sofit.user.domain.loan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.LoanStepService;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(LoanStepController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LoanStepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanStepService loanStepService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // === POST /api/loan-applications/{applicationId}/consents ===

    @Test
    @DisplayName("대출 약관 동의 - 200 + LOAN2013 반환")
    void processConsent_returns200() throws Exception {
        // given
        ConsentCreateResponse response = new ConsentCreateResponse(
                TermType.LOAN_APPLICATION, APPLICATION_ID, USER_ID,
                List.of(new ConsentItemResponse(1L, true, LocalDateTime.now())));

        given(loanStepService.processConsent(anyLong(), anyLong(), any())).willReturn(response);

        String requestBody = """
                {
                    "termType": "LOAN_APPLICATION",
                    "applicationId": 100,
                    "consents": [
                        {"termId": 1, "isConsented": true}
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/consents", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2013"))
                .andExpect(jsonPath("$.result.termType").value("LOAN_APPLICATION"));
    }

    @Test
    @DisplayName("대출 약관 동의 - 단계 순서 위반이면 400 반환")
    void processConsent_returns400_whenStepOrderViolation() throws Exception {
        // given
        given(loanStepService.processConsent(anyLong(), anyLong(), any()))
                .willThrow(new BaseException(LoanErrorCode.STEP_ORDER_VIOLATION));

        String requestBody = """
                {
                    "termType": "LOAN_APPLICATION",
                    "applicationId": 100,
                    "consents": [
                        {"termId": 1, "isConsented": true}
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/consents", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4003"))
                .andExpect(jsonPath("$.message").value("이전 단계를 먼저 완료해야 합니다."));
    }

    // === POST /api/loan-applications/{applicationId}/biz-info ===

    @Test
    @DisplayName("사업자 정보 확인 - 200 + LOAN2014 반환")
    void processBizInfo_returns200() throws Exception {
        // given
        BusinessProfileResponse response = new BusinessProfileResponse(
                "1234567890", "홍길동 가게", "홍길동", "9901011",
                LocalDate.of(2020, 1, 1), "음식점업", "일반음식점", "서울시 강남구", true);

        given(loanStepService.processBizInfo(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/biz-info", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2014"))
                .andExpect(jsonPath("$.result.businessNumber").value("1234567890"))
                .andExpect(jsonPath("$.result.businessName").value("홍길동 가게"));
    }

    @Test
    @DisplayName("사업자 정보 확인 - 신청 미존재이면 404 반환")
    void processBizInfo_returns404_whenNotFound() throws Exception {
        // given
        given(loanStepService.processBizInfo(anyLong(), anyLong()))
                .willThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/biz-info", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4042"));
    }

    // === POST /api/loan-applications/{applicationId}/mydata ===

    @Test
    @DisplayName("마이데이터 약관 동의 - 200 + LOAN2015 반환")
    void processMydata_returns200() throws Exception {
        // given
        ConsentCreateResponse response = new ConsentCreateResponse(
                TermType.MYDATA, APPLICATION_ID, USER_ID,
                List.of(new ConsentItemResponse(2L, true, LocalDateTime.now())));

        given(loanStepService.processMydata(anyLong(), anyLong(), any())).willReturn(response);

        String requestBody = """
                {
                    "termType": "MYDATA",
                    "applicationId": 100,
                    "consents": [
                        {"termId": 2, "isConsented": true}
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/mydata", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2015"))
                .andExpect(jsonPath("$.result.termType").value("MYDATA"));
    }

    // === POST /api/loan-applications/{applicationId}/mybiz-data ===

    @Test
    @DisplayName("마이비즈데이터 연동 완료 - 200 + LOAN2016 반환")
    void processMybizData_returns200() throws Exception {
        // given
        doNothing().when(loanStepService).processMybizData(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/mybiz-data", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("LOAN2016"));
    }

    @Test
    @DisplayName("마이비즈데이터 연동 - 본인 소유가 아니면 403 반환")
    void processMybizData_returns403_whenNotOwned() throws Exception {
        // given
        doThrow(new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED))
                .when(loanStepService).processMybizData(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/api/loan-applications/{applicationId}/mybiz-data", APPLICATION_ID)
                        .sessionAttr("userId", USER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOAN4032"))
                .andExpect(jsonPath("$.message").value("본인의 대출 신청만 처리할 수 있습니다."));
    }
}
