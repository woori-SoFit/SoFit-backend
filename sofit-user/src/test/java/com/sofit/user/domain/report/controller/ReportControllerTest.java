package com.sofit.user.domain.report.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.dto.response.MybizStatusResponse;
import com.sofit.user.domain.report.exception.ReportErrorCode;
import com.sofit.user.domain.report.service.ReportService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;

    /**
     * SecurityUtil.getCurrentUserId()는 SecurityContext의 Authentication에서 userId(Long)를 읽는다.
     * 각 테스트 전에 SecurityContext에 userId를 principal로 설정한다.
     */
    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ===================== GET /api/report/grade =====================

    @Test
    @DisplayName("GET /api/report/grade - 성공 시 200 + REPORT2000 응답을 반환한다")
    void findGrade_success_returns200() throws Exception {
        // given
        GradeResponse gradeResponse = new GradeResponse(
                1L,
                USER_ID,
                "S3",
                "안정적으로 성장하고 있는 우수 사업장입니다.",
                "지속적인 매출 성장과 안정적인 상권을 기반으로 신용도가 향상돼요.",
                LocalDateTime.of(2024, 5, 15, 9, 0, 0)
        );

        given(reportService.findGrade(USER_ID)).willReturn(gradeResponse);

        // when & then
        mockMvc.perform(get("/api/report/grade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT2000"))
                .andExpect(jsonPath("$.message").value("성장 S등급 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.evaluationId").value(1))
                .andExpect(jsonPath("$.result.userId").value(USER_ID))
                .andExpect(jsonPath("$.result.sGrade").value("S3"))
                .andExpect(jsonPath("$.result.comment").value("안정적으로 성장하고 있는 우수 사업장입니다."))
                .andExpect(jsonPath("$.result.commentDetail").isNotEmpty())
                .andExpect(jsonPath("$.result.createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/report/grade - 성장 S등급 미산출 시 404 + REPORT4040 응답을 반환한다")
    void findGrade_gradeNotFound_returns404() throws Exception {
        // given
        given(reportService.findGrade(USER_ID))
                .willThrow(new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/report/grade"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("REPORT4040"))
                .andExpect(jsonPath("$.message").value("아직 성장 S등급이 산출되지 않았습니다."));
    }

    // ===================== GET /api/report/detail =====================

    @Test
    @DisplayName("GET /api/report/detail - 성공 시 200 + REPORT2001 응답을 반환한다")
    void findGradeDetail_success_returns200() throws Exception {
        // given
        GradeDetailResponse detailResponse = new GradeDetailResponse(
                "S5",
                List.of("매출 성장", "고객 재방문율"),
                List.of("현금흐름 관리"),
                "현금흐름 안정화를 통해 등급을 높이세요."
        );

        given(reportService.findGradeDetail(USER_ID)).willReturn(detailResponse);

        // when & then
        mockMvc.perform(get("/api/report/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT2001"))
                .andExpect(jsonPath("$.message").value("성장 S등급 상세 리포트 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.sGrade").value("S5"))
                .andExpect(jsonPath("$.result.strengthKeywords").isArray())
                .andExpect(jsonPath("$.result.strengthKeywords[0]").value("매출 성장"))
                .andExpect(jsonPath("$.result.strengthKeywords[1]").value("고객 재방문율"))
                .andExpect(jsonPath("$.result.improvementKeywords").isArray())
                .andExpect(jsonPath("$.result.improvementKeywords[0]").value("현금흐름 관리"))
                .andExpect(jsonPath("$.result.advice").value("현금흐름 안정화를 통해 등급을 높이세요."));
    }

    @Test
    @DisplayName("GET /api/report/detail - strengthKeywords와 improvementKeywords가 빈 배열이어도 정상 응답한다")
    void findGradeDetail_withEmptyKeywords_returns200() throws Exception {
        // given
        GradeDetailResponse detailResponse = new GradeDetailResponse(
                "S1",
                List.of(),
                List.of(),
                "최고 수준을 유지하세요."
        );

        given(reportService.findGradeDetail(USER_ID)).willReturn(detailResponse);

        // when & then
        mockMvc.perform(get("/api/report/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sGrade").value("S1"))
                .andExpect(jsonPath("$.result.strengthKeywords").isArray())
                .andExpect(jsonPath("$.result.strengthKeywords").isEmpty())
                .andExpect(jsonPath("$.result.improvementKeywords").isArray())
                .andExpect(jsonPath("$.result.improvementKeywords").isEmpty());
    }

    @Test
    @DisplayName("GET /api/report/detail - 성장 S등급 미산출 시 404 + REPORT4040 응답을 반환한다")
    void findGradeDetail_gradeNotFound_returns404() throws Exception {
        // given
        given(reportService.findGradeDetail(USER_ID))
                .willThrow(new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/report/detail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("REPORT4040"))
                .andExpect(jsonPath("$.message").value("아직 성장 S등급이 산출되지 않았습니다."));
    }

    // ===================== GET /api/report/mybiz-status =====================

    @Test
    @DisplayName("GET /api/report/mybiz-status - 마이비즈 연동 완료 시 200 + isMybizConnected=true 반환한다")
    void findMybizStatus_whenConnected_returnsTrue() throws Exception {
        // given
        MybizStatusResponse statusResponse = new MybizStatusResponse(true);
        given(reportService.findMybizStatus(USER_ID)).willReturn(statusResponse);

        // when & then
        mockMvc.perform(get("/api/report/mybiz-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT2002"))
                .andExpect(jsonPath("$.message").value("마이비즈 연동 여부 확인에 성공했습니다."))
                .andExpect(jsonPath("$.result.isMybizConnected").value(true));
    }

    @Test
    @DisplayName("GET /api/report/mybiz-status - 마이비즈 미연동 시 200 + isMybizConnected=false 반환한다")
    void findMybizStatus_whenNotConnected_returnsFalse() throws Exception {
        // given
        MybizStatusResponse statusResponse = new MybizStatusResponse(false);
        given(reportService.findMybizStatus(USER_ID)).willReturn(statusResponse);

        // when & then
        mockMvc.perform(get("/api/report/mybiz-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT2002"))
                .andExpect(jsonPath("$.result.isMybizConnected").value(false));
    }
}
