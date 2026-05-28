package com.sofit.user.domain.report.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.report.ShapExplanation;
import com.sofit.common.repository.ShapExplanationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
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

    private final ShapExplanationRepository shapExplanationRepository;
    private final BusinessProfileRepository businessProfileRepository;

    @Override
    public GradeResponse findGrade(Long userId) {
        ShapExplanation explanation = shapExplanationRepository
                .findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        return ReportConverter.toGradeResponse(explanation);
    }

    @Override
    public GradeDetailResponse findGradeDetail(Long userId) {
        ShapExplanation explanation = shapExplanationRepository
                .findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BaseException(ReportErrorCode.GRADE_NOT_FOUND));

        return ReportConverter.toGradeDetailResponse(explanation);
    }

    @Override
    public MybizStatusResponse findMybizStatus(Long userId) {
        boolean connected = businessProfileRepository.findByUser_UserId(userId)
                .map(BusinessProfile::isMybizConnected)
                .orElse(false);

        return new MybizStatusResponse(connected);
    }
}
