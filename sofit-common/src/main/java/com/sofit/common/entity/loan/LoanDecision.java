package com.sofit.common.entity.loan;

import java.math.BigDecimal;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.loan.enums.Decision;
import com.sofit.common.entity.loan.enums.RepaymentMethod;

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
public class LoanDecision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_id")
    private Long decisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private Decision decision;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "approved_rate", precision = 5, scale = 2)
    private BigDecimal approvedRate;

    @Column(name = "approved_term")
    private Integer approvedTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method")
    private RepaymentMethod repaymentMethod;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // === 정적 팩토리 메서드 ===

    public static LoanDecision createApproval(LoanApplication application,
                                              Long approvedAmount,
                                              BigDecimal approvedRate,
                                              Integer approvedTerm,
                                              RepaymentMethod repaymentMethod,
                                              String comment,
                                              Long createdByUserId) {
        LoanDecision decision = new LoanDecision();
        decision.application = application;
        decision.decision = Decision.APPROVED;
        decision.approvedAmount = approvedAmount;
        decision.approvedRate = approvedRate;
        decision.approvedTerm = approvedTerm;
        decision.repaymentMethod = repaymentMethod;
        decision.comment = comment;
        decision.setCreatedBy(createdByUserId);
        return decision;
    }

    public static LoanDecision createRejection(LoanApplication application,
                                               String comment,
                                               Long createdByUserId) {
        LoanDecision decision = new LoanDecision();
        decision.application = application;
        decision.decision = Decision.REJECTED;
        decision.comment = comment;
        decision.setCreatedBy(createdByUserId);
        return decision;
    }
}
