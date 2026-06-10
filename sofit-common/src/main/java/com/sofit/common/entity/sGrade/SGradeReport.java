package com.sofit.common.entity.sGrade;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.converter.StringDoubleMapConverter;
import com.sofit.common.entity.converter.StringListConverter;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "s_grade_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SGradeReport extends BaseEntity {

    @Id
    @Column(name = "s_grade_id")
    private Long sGradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "feature_id")
    private Long featureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "s_grade", nullable = false)
    private SGrade sGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_grade")
    private SGrade targetGrade;

    @Convert(converter = StringListConverter.class)
    @Column(name = "strength_keywords", columnDefinition = "TEXT")
    private List<String> strengthKeywords;

    @Convert(converter = StringListConverter.class)
    @Column(name = "improvement_keywords", columnDefinition = "TEXT")
    private List<String> improvementKeywords;

    @Convert(converter = StringDoubleMapConverter.class)
    @Column(name = "strength_details", columnDefinition = "TEXT")
    private Map<String, Double> strengthDetails;

    @Convert(converter = StringDoubleMapConverter.class)
    @Column(name = "improvement_details", columnDefinition = "TEXT")
    private Map<String, Double> improvementDetails;

    @Column(name = "user_advice", columnDefinition = "TEXT")
    private String userAdvice;

    @Column(name = "admin_advice", columnDefinition = "TEXT")
    private String adminAdvice;

    /**
     * AI 서버 응답으로부터 SGradeReport를 생성한다.
     */
    public static SGradeReport create(Long sGradeId, User user, Long featureId,
                                       String sGradeValue, String targetGradeValue,
                                       List<String> strengthKeywords, List<String> improvementKeywords,
                                       Map<String, Double> strengthDetails, Map<String, Double> improvementDetails,
                                       String userAdvice, String adminAdvice) {
        SGradeReport report = new SGradeReport();
        report.sGradeId = sGradeId;
        report.user = user;
        report.featureId = featureId;
        report.sGrade = SGrade.valueOf(sGradeValue);
        report.targetGrade = targetGradeValue != null ? SGrade.valueOf(targetGradeValue) : null;
        report.strengthKeywords = strengthKeywords;
        report.improvementKeywords = improvementKeywords;
        report.strengthDetails = strengthDetails;
        report.improvementDetails = improvementDetails;
        report.userAdvice = userAdvice;
        report.adminAdvice = adminAdvice;
        return report;
    }
}
