package com.sofit.user.domain.report.converter;

import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.enums.SGradeComment;

public class ReportConverter {

    private ReportConverter() {}

    /**
     * SGradeReport 엔티티 → GradeResponse 변환.
     * comment, commentDetail은 등급 기반 고정 문자열에서 가져온다.
     */
    public static GradeResponse toGradeResponse(SGradeReport entity) {
        SGradeComment gradeComment = SGradeComment.fromGrade(entity.getSGrade());

        String comment = gradeComment != null ? gradeComment.getComment() : "";
        String commentDetail = gradeComment != null ? gradeComment.getCommentDetail() : "";

        return new GradeResponse(
                entity.getSGradeId(),
                entity.getUser().getUserId(),
                entity.getSGrade().name(),
                comment,
                commentDetail,
                entity.getCreatedAt()
        );
    }

    /**
     * SGradeReport 엔티티 → GradeDetailResponse 변환.
     */
    public static GradeDetailResponse toGradeDetailResponse(SGradeReport entity) {
        return new GradeDetailResponse(
                entity.getSGrade().name(),
                entity.getStrengthKeywords(),
                entity.getImprovementKeywords(),
                entity.getUserAdvice()
        );
    }
}
