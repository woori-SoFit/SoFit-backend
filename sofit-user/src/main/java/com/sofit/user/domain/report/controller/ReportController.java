package com.sofit.user.domain.report.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.exception.ReportSuccessCode;
import com.sofit.user.domain.report.service.ReportService;
import com.sofit.user.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

    private final ReportService reportService;

    @GetMapping("/grade")
    public ApiResponse<GradeResponse> findGrade() {
        Long userId = SecurityUtil.getCurrentUserId();
        GradeResponse response = reportService.findGrade(userId);
        return ApiResponse.onSuccess(ReportSuccessCode.GRADE_OK, response);
    }
}
