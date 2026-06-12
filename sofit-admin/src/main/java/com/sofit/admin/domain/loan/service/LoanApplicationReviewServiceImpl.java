package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanApplicationReviewConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ApplicationInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.DecisionResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ProductInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.RecommendationResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanProductOptionRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.common.audit.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationReviewServiceImpl implements LoanApplicationReviewService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanProductOptionRepository loanProductOptionRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final UserRepository userRepository;

    @Override
    @AuditLog(action = "LOAN_APPLICATION_VIEW", target = "대출 심사 상세 조회")
    public LoanApplicationReviewResponse findLoanApplicationReview(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 2. LoanProduct 추출
        LoanProduct product = application.getProduct();

        // 3. LoanProductOption 목록 조회
        List<LoanProductOption> options = loanProductOptionRepository.findByProduct_ProductId(product.getProductId());

        // 4. LoanDecision 전체 목록 조회 (createdAt 오름차순)
        List<LoanDecision> decisions = loanDecisionRepository
                .findAllByApplication_ApplicationIdOrderByCreatedAtAsc(applicationId);

        // 5. 시스템 심사 추출: DecisionStatus.SYSTEM_APPROVED인 건 → Recommendation
        LoanDecision systemApproved = decisions.stream()
                .filter(d -> d.getStatus() == DecisionStatus.SYSTEM_APPROVED)
                .findFirst()
                .orElse(null);

        // 6. 은행원 심사의 createdBy ID를 수집하여 한 번에 User 조회 (N+1 방지)
        List<Long> createdByIds = decisions.stream()
                .map(LoanDecision::getCreatedBy)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, User> userMap = createdByIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(createdByIds).stream()
                        .collect(Collectors.toMap(User::getUserId, user -> user));

        // 7. DecisionResponse 목록 생성
        List<DecisionResponse> decisionResponses = new ArrayList<>();
        for (LoanDecision decision : decisions) {
            User user = decision.getCreatedBy() != null
                    ? userMap.get(decision.getCreatedBy())
                    : null;
            decisionResponses.add(LoanApplicationReviewConverter.toDecisionResponse(decision, user));
        }

        // 7. Converter로 DTO 변환 후 반환
        ProductInfoResponse productInfo = LoanApplicationReviewConverter.toProductInfoResponse(product, options);
        ApplicationInfoResponse applicationInfo = LoanApplicationReviewConverter.toApplicationInfoResponse(application);
        RecommendationResponse recommendation = LoanApplicationReviewConverter.toRecommendationResponse(systemApproved);

        return LoanApplicationReviewConverter.toLoanApplicationReviewResponse(
                productInfo, applicationInfo, recommendation, decisionResponses);
    }
}
