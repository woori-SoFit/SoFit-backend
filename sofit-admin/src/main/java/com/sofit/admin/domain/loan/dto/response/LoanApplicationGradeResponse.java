package com.sofit.admin.domain.loan.dto.response;

import java.util.List;
import java.util.Map;

public record LoanApplicationGradeResponse(
        CbScoreInfo cbScore,
        String sGrade,
        ScbInfo scbInfo,
        ShapResult shapResult
) {
    public record CbScoreInfo(
            Integer score,
            Integer maxScore
    ) {}

    public record ScbInfo(
            Integer score,
            Integer maxScore,
            Integer bonusPoints
    ) {}

    public record ShapResult(
            String grade,
            String targetGrade,
            List<String> strengthKeywords,
            List<String> improvementKeywords,
            Map<String, Double> strengthDetails,
            Map<String, Double> improvementDetails,
            String advice
    ) {}
}
