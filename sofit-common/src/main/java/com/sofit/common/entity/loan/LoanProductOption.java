package com.sofit.common.entity.loan;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan_product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanProductOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private LoanPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Column(name = "max_term_months", nullable = false)
    private Integer maxTermMonths;
}
