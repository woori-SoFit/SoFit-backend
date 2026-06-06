package com.sofit.user.domain.report.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.user.domain.report.converter.ReportConverter;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.dto.response.MybizStatusResponse;
import com.sofit.user.domain.report.exception.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final SGradeReportRepository sGradeReportRepository;
    private final BusinessProfileRepository businessProfileRepository;

    @Override
    public GradeResponse findGrade(Long userId) {
        SGradeReport sGradeReport = sGradeReportRepository
                .findLatestCompletedByUserId(userId)
                .orElseThrow(() -> new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        return ReportConverter.toGradeResponse(sGradeReport);
    }

    @Override
    public GradeDetailResponse findGradeDetail(Long userId) {
        SGradeReport sGradeReport = sGradeReportRepository
                .findLatestCompletedByUserId(userId)
                .orElseThrow(() -> new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        return ReportConverter.toGradeDetailResponse(sGradeReport);
    }

    @Override
    public MybizStatusResponse findMybizStatus(Long userId) {
        boolean connected = businessProfileRepository.findByUser_UserId(userId)
                .map(BusinessProfile::isMybizConnected)
                .orElse(false);

        return new MybizStatusResponse(connected);
    }
}
