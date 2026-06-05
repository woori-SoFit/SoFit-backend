package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.entity.sGrade.enums.SGrade;

import java.util.Collections;
import java.util.Map;

public class LoanApplicationGradeConverter {

    private static final int MAX_SCORE = 1000;

    private LoanApplicationGradeConverter() {
    }

    public static LoanApplicationGradeResponse toLoanApplicationGradeResponse(
            Scb scb, SGrade sGrade, SGradeReport sGradeReport) {

        // cbScore 섹션
        LoanApplicationGradeResponse.CbScoreInfo cbScore =
                new LoanApplicationGradeResponse.CbScoreInfo(scb.getCbScore(), MAX_SCORE);

        // scbInfo 섹션
        LoanApplicationGradeResponse.ScbInfo scbInfo =
                new LoanApplicationGradeResponse.ScbInfo(
                        scb.getScbScore(), MAX_SCORE, scb.getScoreAddition());

        // shapResult 섹션
        Map<String, Double> strengthDetails = sGradeReport.getStrengthDetails() != null
                ? sGradeReport.getStrengthDetails() : Collections.emptyMap();
        Map<String, Double> improvementDetails = sGradeReport.getImprovementDetails() != null
                ? sGradeReport.getImprovementDetails() : Collections.emptyMap();

        LoanApplicationGradeResponse.ShapResult shapResult =
                new LoanApplicationGradeResponse.ShapResult(
                        sGradeReport.getSGrade().getLabel(),
                        sGradeReport.getTargetGrade() != null
                                ? sGradeReport.getTargetGrade().getLabel() : null,
                        sGradeReport.getStrengthKeywords() != null
                                ? sGradeReport.getStrengthKeywords() : Collections.emptyList(),
                        sGradeReport.getImprovementKeywords() != null
                                ? sGradeReport.getImprovementKeywords() : Collections.emptyList(),
                        strengthDetails,
                        improvementDetails,
                        sGradeReport.getAdminAdvice());

        return new LoanApplicationGradeResponse(cbScore, sGrade.getLabel(), scbInfo, shapResult);
    }
}
