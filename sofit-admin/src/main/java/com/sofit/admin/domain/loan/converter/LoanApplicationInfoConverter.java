package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.user.User;

import java.util.List;

public class LoanApplicationInfoConverter {

    private LoanApplicationInfoConverter() {
    }

    public static LoanApplicationInfoResponse toLoanApplicationInfoResponse(
            User user,
            BusinessProfile businessProfile,
            LoanApplication app,
            List<ConsentHistory> consentHistories) {

        return new LoanApplicationInfoResponse(
                toApplicantInfo(user),
                toBusinessInfo(businessProfile),
                toApplicationInfo(app),
                toUserInputInfo(app),
                toConsentHistories(consentHistories)
        );
    }

    public static LoanApplicationInfoResponse.ApplicantInfo toApplicantInfo(User user) {
        return new LoanApplicationInfoResponse.ApplicantInfo(
                user.getName(),
                user.getResidentNumber(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                user.getLoginId()
        );
    }

    public static LoanApplicationInfoResponse.BusinessInfo toBusinessInfo(BusinessProfile bp) {
        return new LoanApplicationInfoResponse.BusinessInfo(
                bp.getBusinessName(),
                bp.getBusinessNumber(),
                bp.getBusinessCategory(),
                bp.getBusinessType(),
                bp.getBusinessAddress(),
                bp.getOpenDate()
        );
    }

    public static LoanApplicationInfoResponse.ApplicationInfo toApplicationInfo(LoanApplication app) {
        return new LoanApplicationInfoResponse.ApplicationInfo(
                app.getRequestedAmount(),
                app.getRequestedTerm(),
                app.getPurpose() != null ? app.getPurpose().name() : null,
                app.getRepaymentMethod() != null ? app.getRepaymentMethod().name() : null
        );
    }

    public static LoanApplicationInfoResponse.UserInputInfo toUserInputInfo(LoanApplication app) {
        return new LoanApplicationInfoResponse.UserInputInfo(
                app.getUserInputAnnualIncome() != null ? app.getUserInputAnnualIncome() : null,
                app.getUserInputCreditScore() != null ? app.getUserInputCreditScore() : null,
                app.getUserInputIncomeType() != null ? app.getUserInputIncomeType().getCode() : null,
                app.getUserInputExistingLoanAmt() != null ? app.getUserInputExistingLoanAmt() : null
        );
    }

    public static List<LoanApplicationInfoResponse.ConsentHistoryItem> toConsentHistories(
            List<ConsentHistory> consentHistories) {
        if (consentHistories == null || consentHistories.isEmpty()) {
            return List.of();
        }
        return consentHistories.stream()
                .map(LoanApplicationInfoConverter::toConsentHistoryItem)
                .toList();
    }

    public static LoanApplicationInfoResponse.ConsentHistoryItem toConsentHistoryItem(ConsentHistory ch) {
        return new LoanApplicationInfoResponse.ConsentHistoryItem(
                ch.getTerm().getTitle(),
                ch.getTerm().getIsRequired(),
                ch.getIsConsented(),
                ch.getIsConsented() ? ch.getConsentedAt() : null
        );
    }
}
