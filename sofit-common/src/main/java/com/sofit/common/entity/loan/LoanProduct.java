package com.sofit.common.entity.loan;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.loan.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_description", columnDefinition = "TEXT")
    private String targetDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "min_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal minRate;

    @Column(name = "max_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxRate;

    @Column(name = "min_limit", nullable = false)
    private Long minLimit;

    @Column(name = "max_limit", nullable = false)
    private Long maxLimit;

    @Column(name = "min_term", nullable = false)
    private Integer minTerm;

    @Column(name = "max_term", nullable = false)
    private Integer maxTerm;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "subtitle", length = 100)
    private String subtitle;

    @Column(name = "industry_type", length = 50)
    private String industryType;

    @Column(name = "min_business_age_months")
    private Integer minBusinessAgeMonths;

    @Column(name = "annual_income_limit", precision = 18, scale = 0)
    private BigDecimal annualIncomeLimit;

    @Column(name = "income_type_code_limit", length = 2)
    private String incomeTypeCodeLimit;

    @Column(name = "credit_score_limit")
    private Short creditScoreLimit;

    @Column(name = "existing_loan_amt_limit", precision = 18, scale = 0)
    private BigDecimal existingLoanAmtLimit;

    @Column(name = "scb_limit")
    private Integer scbLimit;
}
