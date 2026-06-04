package com.sofit.common.entity.sGrade;

import com.sofit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scb extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scb_id")
    private Long scbId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "cb_score")
    private Integer cbScore;

    @Column(name = "s_grade")
    private String sGrade;

    @Column(name = "score_addition")
    private Integer scoreAddition;

    @Column(name = "scb_score")
    private Integer scbScore;
}
