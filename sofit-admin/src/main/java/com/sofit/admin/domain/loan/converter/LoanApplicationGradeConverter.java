package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.entity.report.Scb;
import com.sofit.common.entity.report.ShapExplanation;
import com.sofit.common.entity.report.enums.SGrade;

import java.util.Collections;
import java.util.Map;

public class LoanApplicationGradeConverter {

    private static final int MAX_SCORE = 1000;

    private LoanApplicationGradeConverter() {
    }

    public static LoanApplicationGradeResponse toLoanApplicationGradeResponse(
            Scb scb, SGrade sGrade, ShapExplanation shapExplanation) {

        // cbScore 섹션
        LoanApplicationGradeResponse.CbScoreInfo cbScore =
                new LoanApplicationGradeResponse.CbScoreInfo(scb.getCbGrade(), MAX_SCORE);

        // scbInfo 섹션
        LoanApplicationGradeResponse.ScbInfo scbInfo =
                new LoanApplicationGradeResponse.ScbInfo(
                        scb.getScbGrade(), MAX_SCORE, scb.getScoreAddition());

        // shapResult 섹션
        Map<String, Double> strengthDetails = shapExplanation.getStrengthDetails() != null
                ? shapExplanation.getStrengthDetails() : Collections.emptyMap();
        Map<String, Double> improvementDetails = shapExplanation.getImprovementDetails() != null
                ? shapExplanation.getImprovementDetails() : Collections.emptyMap();

        LoanApplicationGradeResponse.ShapResult shapResult =
                new LoanApplicationGradeResponse.ShapResult(
                        shapExplanation.getSGrade().getLabel(),
                        shapExplanation.getTargetGrade() != null
                                ? shapExplanation.getTargetGrade().getLabel() : null,
                        shapExplanation.getStrengthKeywords() != null
                                ? shapExplanation.getStrengthKeywords() : Collections.emptyList(),
                        shapExplanation.getImprovementKeywords() != null
                                ? shapExplanation.getImprovementKeywords() : Collections.emptyList(),
                        strengthDetails,
                        improvementDetails,
                        shapExplanation.getAdvice());

        return new LoanApplicationGradeResponse(cbScore, sGrade.getLabel(), scbInfo, shapResult);
    }
}
