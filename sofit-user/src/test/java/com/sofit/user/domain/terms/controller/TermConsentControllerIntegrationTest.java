package com.sofit.user.domain.terms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;
import com.sofit.user.domain.terms.exception.TermErrorCode;
import com.sofit.user.domain.terms.service.TermService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(TermConsentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TermConsentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TermService termService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;

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
    @DisplayName("정상 요청 시 200 + TERM2001 응답을 반환한다")
    void 정상_요청시_200_TERM2001_응답_반환() throws Exception {
        // given
        LocalDateTime consentedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        ConsentCreateResponse response = new ConsentCreateResponse(
                TermType.PERSONAL_INFO,
                null,
                USER_ID,
                List.of(
                        new ConsentItemResponse(1L, true, consentedAt),
                        new ConsentItemResponse(2L, true, consentedAt)
                )
        );

        given(termService.createConsents(any(), any())).willReturn(response);

        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "applicationId": null,
                    "consents": [
                        {"termId": 1, "isConsented": true},
                        {"termId": 2, "isConsented": true}
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("TERM2001"))
                .andExpect(jsonPath("$.message").value("약관 동의가 완료되었습니다."))
                .andExpect(jsonPath("$.result.termType").value("PERSONAL_INFO"))
                .andExpect(jsonPath("$.result.userId").value(USER_ID))
                .andExpect(jsonPath("$.result.consents").isArray())
                .andExpect(jsonPath("$.result.consents.length()").value(2))
                .andExpect(jsonPath("$.result.consents[0].termId").value(1))
                .andExpect(jsonPath("$.result.consents[0].isConsented").value(true))
                .andExpect(jsonPath("$.result.consents[0].consentedAt").exists());
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 응답을 반환한다 - termType 누락")
    void 유효성_검증_실패시_400_응답_반환() throws Exception {
        String requestBody = """
                {
                    "consents": [
                        {"termId": 1, "isConsented": true}
                    ]
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4000"));
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 응답을 반환한다 - consents 비어있음")
    void 유효성_검증_실패시_400_응답_반환_consents_비어있음() throws Exception {
        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "consents": []
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4000"));
    }

    @Test
    @DisplayName("존재하지 않는 약관 요청 시 404 응답을 반환한다")
    void 존재하지_않는_약관_요청시_404_응답_반환() throws Exception {
        given(termService.createConsents(any(), any()))
                .willThrow(new BaseException(TermErrorCode.TERM_NOT_FOUND));

        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "consents": [
                        {"termId": 999, "isConsented": true}
                    ]
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TERM4041"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 약관입니다."));
    }

    @Test
    @DisplayName("termType 불일치 시 400 응답을 반환한다")
    void termType_불일치시_400_응답_반환() throws Exception {
        given(termService.createConsents(any(), any()))
                .willThrow(new BaseException(TermErrorCode.TERM_TYPE_MISMATCH));

        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "consents": [
                        {"termId": 1, "isConsented": true}
                    ]
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TERM4001"))
                .andExpect(jsonPath("$.message").value("약관 유형이 일치하지 않습니다."));
    }

    @Test
    @DisplayName("필수 약관 미동의 시 400 응답을 반환한다")
    void 필수_약관_미동의시_400_응답_반환() throws Exception {
        given(termService.createConsents(any(), any()))
                .willThrow(new BaseException(TermErrorCode.REQUIRED_TERM_NOT_CONSENTED));

        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "consents": [
                        {"termId": 1, "isConsented": false}
                    ]
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TERM4002"))
                .andExpect(jsonPath("$.message").value("필수 약관에 동의하지 않았습니다."));
    }

    @Test
    @DisplayName("이미 동의한 약관에 재동의 시 400 응답을 반환한다")
    void 이미_동의한_약관_재동의시_400_응답_반환() throws Exception {
        given(termService.createConsents(any(), any()))
                .willThrow(new BaseException(TermErrorCode.ALREADY_CONSENTED));

        String requestBody = """
                {
                    "termType": "PERSONAL_INFO",
                    "consents": [
                        {"termId": 1, "isConsented": true}
                    ]
                }
                """;

        mockMvc.perform(post("/api/terms/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TERM4003"))
                .andExpect(jsonPath("$.message").value("이미 동의한 약관입니다."));
    }

}
