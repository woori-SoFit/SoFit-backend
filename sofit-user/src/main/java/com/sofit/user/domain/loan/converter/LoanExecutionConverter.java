package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionItemResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static LoanExecutionItemResponse toItemResponse(LoanExecution execution, LoanDecision decision) {
        LoanApplication app = execution.getApplication();
        LoanProduct product = app.getProduct();

        return new LoanExecutionItemResponse(
                execution.getExecutionId(),
                app.getApplicationId(),
                product.getProductId(),
                product.getProductName(),
                execution.getExecutionAmount(),
                decision.getApprovedRate(),
                decision.getApprovedTerm(),
                app.getRepaymentMethod(),
                execution.getCreatedAt()
        );
    }

    public static LoanExecutionListResponse toListResponse(List<LoanExecution> executions, List<LoanDecision> decisions) {
        // applicationId -> LoanDecision 매핑
        Map<Long, LoanDecision> decisionMap = decisions.stream()
                .collect(Collectors.toMap(
                        d -> d.getApplication().getApplicationId(),
                        d -> d,
                        (d1, d2) -> d2 // 중복 시 최신 것 사용
                ));

        List<LoanExecutionItemResponse> items = executions.stream()
                .filter(e -> decisionMap.containsKey(e.getApplication().getApplicationId()))
                .map(e -> toItemResponse(e, decisionMap.get(e.getApplication().getApplicationId())))
                .toList();

        return new LoanExecutionListResponse(items);
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
