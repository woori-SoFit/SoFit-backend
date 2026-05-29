package com.sofit.admin.domain.loan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanApplicationReviewResponse(
        ProductInfoResponse productInfo,
        ApplicationInfoResponse applicationInfo,
        RecommendationResponse recommendation,
        List<DecisionResponse> decisions
) {

    public record ProductInfoResponse(
            String productName,
            Long minAmount,
            Long maxAmount,
            BigDecimal minInterestRate,
            BigDecimal maxInterestRate,
            Integer minTermMonths,
            Integer maxTermMonths,
            List<String> availableRepaymentMethods,
            List<String> availablePurposes
    ) {}

    public record ApplicationInfoResponse(
            Long requestedAmount,
            Integer requestedTerm,
            String purpose,
            String repaymentMethod
    ) {}

    public record RecommendationResponse(
            Long approvedAmount,
            BigDecimal approvedRate,
            Integer approvedTerm,
            String repaymentMethod
    ) {}

    public record DecisionResponse(
            String status,
            String comment,
            String reviewerName,
            String reviewerRole,
            LocalDateTime decidedAt
    ) {}
}
