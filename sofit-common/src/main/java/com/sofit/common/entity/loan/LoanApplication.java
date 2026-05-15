package com.sofit.common.entity.loan;

import java.time.LocalDateTime;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.loan.enums.AnnualIncome;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.CreditScoreRange;
import com.sofit.common.entity.loan.enums.ExistingLoanAmount;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;

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
@Table(name = "loan_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanApplication extends BaseEntity {

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

    @Column(name = "s_evaluation_id")
    private Long sEvaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_input_annual_income")
    private AnnualIncome userInputAnnualIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_input_credit_score")
    private CreditScoreRange userInputCreditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_input_income_type")
    private IncomeType userInputIncomeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_input_existing_loan_amt")
    private ExistingLoanAmount userInputExistingLoanAmt;

    @Column(name = "requested_amount")
    private Long requestedAmount;

    @Column(name = "requested_term")
    private Integer requestedTerm;

    @Column(name = "cb_credit_score")
    private Integer cbCreditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private LoanPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method")
    private RepaymentMethod repaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    // === 비즈니스 메서드 ===

    public static LoanApplication createDraft(User user, LoanProduct product,
                                              AnnualIncome annualIncome,
                                              CreditScoreRange creditScore,
                                              IncomeType incomeType,
                                              ExistingLoanAmount existingLoanAmt) {
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
}
