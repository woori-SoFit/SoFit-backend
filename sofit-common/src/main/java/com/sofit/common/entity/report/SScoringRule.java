package com.sofit.common.entity.report;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "s_scoring_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SScoringRule {

    @Id
    @Column(name = "grade")
    private String grade;

    @Column(name = "score_addition")
    private Integer scoreAddition;

    @Column(name = "description")
    private String description;
}
