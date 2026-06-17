package com.sofit.common.entity.loan;

import com.sofit.common.entity.TraceableEntity;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanApplication extends TraceableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Column(name = "biz_data_id")
    private Long bizDataId;

    @Column(name = "s_grade_id")
    private Long sGradeId;

    @Column(name = "user_input_annual_income")
    private String userInputAnnualIncome;

    @Column(name = "user_input_credit_score")
    private String userInputCreditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_input_income_type")
    private IncomeType userInputIncomeType;

    @Column(name = "user_input_existing_loan_amt")
    private String userInputExistingLoanAmt;

    @Column(name = "requested_amount")
    private Long requestedAmount;

    @Column(name = "requested_term")
    private Integer requestedTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private LoanPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method")
    private RepaymentMethod repaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_completed_step", length = 30)
    private LastCompletedStep lastCompletedStep;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "assigned_banker_id")
    private Long assignedBankerId;

    // === 비즈니스 메서드 ===

    public static LoanApplication createDraft(User user, LoanProduct product,
                                              String annualIncome,
                                              String creditScore,
                                              IncomeType incomeType,
                                              String existingLoanAmt) {
        LoanApplication application = new LoanApplication();
        application.user = user;
        application.product = product;
        application.userInputAnnualIncome = annualIncome;
        application.userInputCreditScore = creditScore;
        application.userInputIncomeType = incomeType;
        application.userInputExistingLoanAmt = existingLoanAmt;
        application.status = ApplicationStatus.DRAFT;
        return application;
    }

    public void submit(Long requestedAmount, Integer requestedTerm,
                       RepaymentMethod repaymentMethod, LoanPurpose purpose) {
        this.requestedAmount = requestedAmount;
        this.requestedTerm = requestedTerm;
        this.repaymentMethod = repaymentMethod;
        this.purpose = purpose;
        this.status = ApplicationStatus.SUBMITTED;
        this.appliedAt = LocalDateTime.now();
    }

    public void updateStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
    }

    public void updateLastCompletedStep(LastCompletedStep step) {
        this.lastCompletedStep = step;
    }

    public void assignBanker(Long bankerId) {
        this.assignedBankerId = bankerId;
    }

    public void updateBizDataId(Long bizDataId) {
        this.bizDataId = bizDataId;
    }

    public void updateSGradeId(Long sGradeId) {
        this.sGradeId = sGradeId;
    }
}
