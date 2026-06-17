package com.sofit.user.domain.sgrade.converter;

import com.sofit.common.entity.sGrade.SGradeHistory;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.user.domain.sgrade.dto.SGradePredictResponse;

public class SGradeConverter {

    private SGradeConverter() {
    }

    public static SGradeReport toSGradeReport(SGradeHistory history, SGradePredictResponse response) {
        return SGradeReport.create(
                history.getSGradeId(),
                history.getUser(),
                history.getFeatureId(),
                response.sGrade(),
                response.targetGrade(),
                response.strengthKeywords(),
                response.improvementKeywords(),
                response.strengthDetails(),
                response.improvementDetails(),
                response.userAdvice(),
                response.adminAdvice()
        );
    }
}
