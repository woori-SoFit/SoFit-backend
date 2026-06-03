package com.sofit.user.domain.terms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import com.sofit.user.domain.terms.service.TermService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(TermController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TermControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TermService termService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    @Test
    @DisplayName("termType으로 약관 목록 조회 시 200 + TERM2000 응답을 반환한다")
    void getTerms_returns200() throws Exception {
        // given
        TermListResponse response = new TermListResponse(List.of(
                new TermListResponse.TermItem(
                        1L, "PERSONAL_INFO", "v1.0", "개인정보 수집 동의",
                        "/terms/personal_info_v1.0.pdf", true,
                        LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        given(termService.findTerms(eq(TermType.PERSONAL_INFO))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/terms").param("termType", "PERSONAL_INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("TERM2000"))
                .andExpect(jsonPath("$.result.terms[0].termId").value(1L));
    }

    @Test
    @DisplayName("termType 파라미터가 없으면 400을 반환한다")
    void getTerms_missingTermType_returns400() throws Exception {
        // when & then
        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 termType이면 400을 반환한다")
    void getTerms_invalidTermType_returns400() throws Exception {
        // when & then
        mockMvc.perform(get("/api/terms").param("termType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
    }
}
