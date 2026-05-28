package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

public class LoanExecutionConverter {

    private LoanExecutionConverter() {}

    public static LoanExecutionResultResponse toResponse(LoanExecution execution, LoanDecision decision) {
        LoanApplication app = execution.getApplication();
        LoanProduct product = app.getProduct();

        return new LoanExecutionResultResponse(
                execution.getExecutionId(),
                app.getApplicationId(),
                product.getProductId(),
                product.getProductName(),
                execution.getExecutionAmount(),
                decision.getApprovedRate(),
                decision.getApprovedTerm(),
                app.getRepaymentMethod()
        );
    }

    /**
     * 1원 송금 요청 성공 후 응답 변환
     */
    public static AccountVerificationResponse toVerificationResponse(String maskedAccountNumber, String authCode, String expiredAt) {
        return new AccountVerificationResponse(maskedAccountNumber, authCode, expiredAt);
    }

    /**
     * 계좌 인증 확인 후 응답 변환
     */
    public static AccountVerificationConfirmResponse toVerificationConfirmResponse(boolean accountVerified) {
        return new AccountVerificationConfirmResponse(accountVerified);
    }
}
