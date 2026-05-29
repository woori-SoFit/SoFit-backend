package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ApplicationInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.DecisionResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ProductInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.RecommendationResponse;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.common.entity.loan.enums.Decision;
import com.sofit.common.entity.user.User;

import java.util.List;
import java.util.stream.Collectors;

public class LoanApplicationReviewConverter {

    private LoanApplicationReviewConverter() {
    }

    /**
     * LoanProduct + LoanProductOption 목록을 ProductInfoResponse로 변환한다.
     * availableRepaymentMethods, availablePurposes는 중복 제거하여 리스트로 변환한다.
     */
    public static ProductInfoResponse toProductInfoResponse(LoanProduct product, List<LoanProductOption> options) {
        List<String> availableRepaymentMethods = options.stream()
                .map(option -> option.getRepaymentMethod().name())
                .distinct()
                .collect(Collectors.toList());

        List<String> availablePurposes = options.stream()
                .map(option -> option.getPurpose().name())
                .distinct()
                .collect(Collectors.toList());

        return new ProductInfoResponse(
                product.getProductName(),
                product.getMinLimit(),
                product.getMaxLimit(),
                product.getMinRate(),
                product.getMaxRate(),
                product.getMinTerm(),
                product.getMaxTerm(),
                availableRepaymentMethods,
                availablePurposes
        );
    }

    /**
     * LoanApplication을 ApplicationInfoResponse로 변환한다.
     * null 필드는 null로 유지한다.
     */
    public static ApplicationInfoResponse toApplicationInfoResponse(LoanApplication application) {
        return new ApplicationInfoResponse(
                application.getRequestedAmount(),
                application.getRequestedTerm(),
                application.getPurpose() != null ? application.getPurpose().name() : null,
                application.getRepaymentMethod() != null ? application.getRepaymentMethod().name() : null
        );
    }

    /**
     * 시스템 심사(created_by == null) 중 APPROVED인 LoanDecision을 RecommendationResponse로 변환한다.
     * created_by != null이거나 decision != APPROVED이면 null을 반환한다.
     */
    public static RecommendationResponse toRecommendationResponse(LoanDecision decision) {
        if (decision == null) {
            return null;
        }
        if (decision.getCreatedBy() != null || decision.getDecision() != Decision.APPROVED) {
            return null;
        }
        return new RecommendationResponse(
                decision.getApprovedAmount(),
                decision.getApprovedRate(),
                decision.getApprovedTerm(),
                decision.getRepaymentMethod() != null ? decision.getRepaymentMethod().name() : null
        );
    }

    /**
     * LoanDecision + User를 DecisionResponse로 변환한다.
     * - created_by == null: reviewerName="시스템", reviewerRole="SYSTEM", APPROVED면 status="SYSTEM_APPROVED"
     * - created_by != null && User 존재: reviewerName=user.name, reviewerRole=user.role.name()
     * - created_by != null && User 미존재: reviewerName="알 수 없음", reviewerRole="SYSTEM"
     */
    public static DecisionResponse toDecisionResponse(LoanDecision decision, User user) {
        String status;
        String reviewerName;
        String reviewerRole;

        if (decision.getCreatedBy() == null) {
            // 시스템 심사
            reviewerName = "시스템";
            reviewerRole = "SYSTEM";
            status = decision.getDecision() == Decision.APPROVED ? "SYSTEM_APPROVED" : decision.getDecision().name();
        } else if (user != null) {
            // 은행원 심사 - 사용자 존재
            reviewerName = user.getName();
            reviewerRole = user.getRole().name();
            status = decision.getDecision().name();
        } else {
            // 은행원 심사 - 사용자 미존재
            reviewerName = "알 수 없음";
            reviewerRole = "SYSTEM";
            status = decision.getDecision().name();
        }

        return new DecisionResponse(
                status,
                decision.getComment(),
                reviewerName,
                reviewerRole,
                decision.getCreatedAt()
        );
    }

    /**
     * 전체 응답을 조합하여 LoanApplicationReviewResponse를 생성한다.
     */
    public static LoanApplicationReviewResponse toLoanApplicationReviewResponse(
            ProductInfoResponse productInfo,
            ApplicationInfoResponse applicationInfo,
            RecommendationResponse recommendation,
            List<DecisionResponse> decisions) {

        return new LoanApplicationReviewResponse(
                productInfo,
                applicationInfo,
                recommendation,
                decisions
        );
    }
}
