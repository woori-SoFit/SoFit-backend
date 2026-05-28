package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanApplicationGradeConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.report.Scb;
import com.sofit.common.entity.report.ShapExplanation;
import com.sofit.common.entity.report.enums.SGrade;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.ScbRepository;
import com.sofit.common.repository.ShapExplanationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationGradeServiceImpl implements LoanApplicationGradeService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ScbRepository scbRepository;
    private final ShapExplanationRepository shapExplanationRepository;

    @Override
    public LoanApplicationGradeResponse findLoanApplicationGrade(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 2. Scb 조회 (application_id로)
        Scb scb = scbRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 3. s_grade → SGrade 변환
        SGrade sGrade = convertToSGrade(scb.getSGrade());

        // 4. ShapExplanation 조회 (s_evaluation_id로)
        Long sEvaluationId = app.getSEvaluationId();
        if (sEvaluationId == null) {
            throw new BaseException(GeneralErrorCode.NOT_FOUND);
        }
        ShapExplanation shapExplanation = shapExplanationRepository.findById(sEvaluationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 5. Converter로 DTO 변환
        return LoanApplicationGradeConverter.toLoanApplicationGradeResponse(
                scb, sGrade, shapExplanation);
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
