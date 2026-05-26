package com.sofit.user.domain.report.converter;

import com.sofit.common.entity.report.ShapExplanation;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.enums.SGradeComment;

public class ReportConverter {

    private ReportConverter() {}

    /**
     * ShapExplanation 엔티티 → GradeResponse 변환.
     * comment, commentDetail은 등급 기반 고정 문자열에서 가져온다.
     */
    public static GradeResponse toGradeResponse(ShapExplanation entity) {
        SGradeComment gradeComment = SGradeComment.fromGrade(entity.getSGrade());

        String comment = gradeComment != null ? gradeComment.getComment() : "";
        String commentDetail = gradeComment != null ? gradeComment.getCommentDetail() : "";

        return new GradeResponse(
                entity.getEvaluationId(),
                entity.getUser().getUserId(),
                entity.getSGrade(),
                comment,
                commentDetail,
                entity.getCreatedAt()
        );
    }
}
