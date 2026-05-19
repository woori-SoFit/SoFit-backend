package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;

public class LoanConverter {

    private LoanConverter() {}

    /**
     * 목록 조회용 Item 변환
     * appliedAt: LocalDateTime → LocalDate (날짜만 반환)
     */
    public static LoanApplicationListResponse.LoanApplicationItem toListItem(LoanApplication application) {
        return LoanApplicationListResponse.LoanApplicationItem.builder()
                .applicationId(application.getApplicationId())
                .productName(application.getProduct().getProductName())
                .status(application.getStatus())
                .requestedAmount(application.getRequestedAmount())
                .appliedAt(application.getAppliedAt() != null
                        ? application.getAppliedAt().toLocalDate()
                        : null)
                .build();
    }

    /**
     * 상세 조회용 DTO 변환
     */
    public static LoanApplicationDetailResponse toDetailResponse(LoanApplication application) {
        return LoanApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .productName(application.getProduct().getProductName())
                .status(application.getStatus())
                .requestedAmount(application.getRequestedAmount())
                .requestedTerm(application.getRequestedTerm())
                .repaymentMethod(application.getRepaymentMethod())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    /**
     * 심사 완료 목록 Item 변환
     */
    public static CompletedLoanListResponse.CompletedLoanItem toCompletedListItem(LoanApplication application) {
        return new CompletedLoanListResponse.CompletedLoanItem(
                application.getApplicationId(),
                application.getProduct().getProductName(),
                application.getStatus(),
                application.getRequestedAmount(),
                application.getAppliedAt() != null
                        ? application.getAppliedAt().toLocalDate()
                        : null,
                application.getUpdatedAt() != null
                        ? application.getUpdatedAt().toLocalDate()
                        : null
        );
    }

    /**
     * 심사 완료 상세 조회용 변환
     */
    public static CompletedLoanDetailResponse toCompletedDetailResponse(LoanApplication application,
                                                                        LoanDecision decision) {
        CompletedLoanDetailResponse.DecisionInfo decisionInfo = new CompletedLoanDetailResponse.DecisionInfo(
                decision.getDecision(),
                decision.getApprovedAmount(),
                decision.getApprovedRate(),
                decision.getApprovedTerm(),
                decision.getRejectionReason()
        );

        return new CompletedLoanDetailResponse(
                application.getApplicationId(),
                application.getProduct().getProductName(),
                application.getRequestedAmount(),
                application.getRepaymentMethod(),
                decisionInfo
        );
    }
}
