package com.sofit.user.domain.loan.dto.response;

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
    private InterestRate interestRate;

    @Getter
    @Builder
    public static class InterestRate {
        private BigDecimal minRate;
        private BigDecimal maxRate;
    }
}
