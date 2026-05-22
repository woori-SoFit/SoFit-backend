package com.sofit.user.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.IncomeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LoanProductDetailResponse {

    private Long productId;
    private String productName;
    private String title;
    private String subtitle;
    private Long minLimit;
    private Long maxLimit;
    private Integer maxTerm;
    private String targetDescription;
    private FilterConditions filterConditions;
    private InterestRate interestRate;

    @Getter
    @Builder
    public static class FilterConditions {
        private BigDecimal annualIncomeLimit;
        private Short creditScoreLimit;
        private IncomeType incomeTypeCodeLimit;
        private BigDecimal existingLoanAmtLimit;
    }

    @Getter
    @Builder
    public static class InterestRate {
        private BigDecimal minRate;
        private BigDecimal maxRate;
    }
}
