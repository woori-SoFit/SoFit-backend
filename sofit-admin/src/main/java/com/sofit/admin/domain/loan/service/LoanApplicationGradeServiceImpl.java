package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanApplicationGradeConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationGradeServiceImpl implements LoanApplicationGradeService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ScbRepository scbRepository;
    private final SGradeReportRepository sGradeReportRepository;

    @Override
    public LoanApplicationGradeResponse findLoanApplicationGrade(Long applicationId) {
        // 1. Scb 조회 (application_id로)
        Scb scb = scbRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 2. s_grade → SGrade 변환
        SGrade sGrade = convertToSGrade(scb.getSGrade());

        // 3. s_grade_id 조회 (LoanApplication 전체 로딩 없이 필요한 필드만)
        Long sGradeId = loanApplicationRepository.findSGradeIdByApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 4. SGradeReport 조회
        SGradeReport sGradeReport = sGradeReportRepository.findById(sGradeId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 5. Converter로 DTO 변환
        return LoanApplicationGradeConverter.toLoanApplicationGradeResponse(
                scb, sGrade, sGradeReport);
    }

    private SGrade convertToSGrade(String sGradeValue) {
        if (sGradeValue == null || sGradeValue.isBlank()) {
            throw new BaseException(GeneralErrorCode.NOT_FOUND);
        }
        try {
            return SGrade.valueOf(sGradeValue);
        } catch (IllegalArgumentException e) {
            throw new BaseException(GeneralErrorCode.NOT_FOUND);
        }
    }
}
