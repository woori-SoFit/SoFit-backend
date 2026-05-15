package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;

public class LoanConverter {

    private LoanConverter() {}

    /**
     * 목록 조회용 DTO 변환
     * appliedAt: LocalDateTime → LocalDate (날짜만 반환)
     */
    public static LoanApplicationListResponse toListResponse(LoanApplication application) {
        return LoanApplicationListResponse.builder()
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
}
