package com.sofit.common.entity.loan;

import java.math.BigDecimal;

import com.sofit.common.entity.loan.enums.DecisionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan_decision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_id")
    private Long decisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private DecisionType decision;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "approved_rate", precision = 5, scale = 2)
    private BigDecimal approvedRate;

    @Column(name = "approved_term")
    private Integer approvedTerm;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
