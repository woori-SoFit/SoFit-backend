package com.sofit.admin.domain.loan.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LoanApplicationInfoResponse(
        ApplicantInfo applicantInfo,
        BusinessInfo businessInfo,
        ApplicationInfo applicationInfo,
        UserInputInfo userInputInfo,
        List<ConsentHistoryItem> consentHistories
) {
    public record ApplicantInfo(
            String name,
            String residentNumber,
            String phoneNumber,
            LocalDateTime joinedAt,
            String loginId
    ) {}

    public record BusinessInfo(
            String businessName,
            String businessNumber,
            String businessCategory,
            String businessType,
            String businessAddress,
            LocalDate openDate
    ) {}

    public record ApplicationInfo(
            Long requestedAmount,
            Integer requestedTerm,
            String purpose,
            String repaymentMethod
    ) {}

    public record UserInputInfo(
            String annualIncome,
            String creditScore,
            String incomeType,
            String existingLoanAmount
    ) {}

    public record ConsentHistoryItem(
            String title,
            Boolean isRequired,
            Boolean isConsented,
            LocalDateTime consentedAt
    ) {}
}
