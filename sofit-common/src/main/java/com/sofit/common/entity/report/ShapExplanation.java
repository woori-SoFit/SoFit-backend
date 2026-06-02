package com.sofit.common.entity.report;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.converter.StringDoubleMapConverter;
import com.sofit.common.entity.converter.StringListConverter;
import com.sofit.common.entity.report.enums.SGrade;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "shap_explanation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShapExplanation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @Column(name = "advice", columnDefinition = "TEXT")
    private String advice;
}
