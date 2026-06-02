package com.sofit.common.entity.report;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.user.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "cb_grade")
    private Integer cbGrade;

    @Column(name = "s_grade")
    private String sGrade;

    @Column(name = "score_addition")
    private Integer scoreAddition;

    @Column(name = "scb_grade")
    private Integer scbGrade;
}
