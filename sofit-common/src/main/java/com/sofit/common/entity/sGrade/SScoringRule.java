package com.sofit.common.entity.sGrade;

import com.sofit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "s_scoring_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SScoringRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rule_id;

    @Column(name = "grade")
    private String grade;

    @Column(name = "score_addition")
    private Integer scoreAddition;

    @Column(name = "description")
    private String description;


}
