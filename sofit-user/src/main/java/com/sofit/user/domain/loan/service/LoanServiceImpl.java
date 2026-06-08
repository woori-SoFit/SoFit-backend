package com.sofit.user.domain.loan.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.user.domain.loan.converter.LoanConverter;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    // 심사 중 상태 목록
    private static final List<ApplicationStatus> UNDER_REVIEW_STATUSES = List.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.CB_CHECKING,
            ApplicationStatus.BASIC_REVIEW,
            ApplicationStatus.S_CALCULATING,
            ApplicationStatus.S_COMPLETED,
            ApplicationStatus.SYSTEM_APPROVED,
            ApplicationStatus.SYSTEM_REJECTED,
            ApplicationStatus.MANAGER_REVIEW
    );

    // 심사 완료 상태 목록
    private static final List<ApplicationStatus> COMPLETED_STATUSES = List.of(
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.EXECUTED
    );

    @Override
    public LoanApplicationListResponse findUnderReviewLoans(Long userId) {
        List<LoanApplicationListResponse.LoanApplicationItem> items = loanApplicationRepository
                .findByUser_UserIdAndStatusIn(userId, UNDER_REVIEW_STATUSES)
                .stream()
                .map(LoanConverter::toListItem)
                .toList();

        return LoanApplicationListResponse.builder()
                .loanApplications(items)
                .build();
    }

    @Override
    public LoanApplicationDetailResponse findLoanDetail(Long userId, Long applicationId) {
        return loanApplicationRepository
                .findByApplicationIdAndUser_UserId(applicationId, userId)
                .map(LoanConverter::toDetailResponse)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));
    }

    @Override
    public CompletedLoanListResponse findCompletedLoans(Long userId) {
        // product fetch join으로 N+1 방지
        List<CompletedLoanListResponse.CompletedLoanItem> items = loanApplicationRepository
                .findCompletedByUserIdWithProduct(userId, COMPLETED_STATUSES)
                .stream()
                .map(LoanConverter::toCompletedListItem)
                .toList();

        return new CompletedLoanListResponse(items);
    }

    @Override
    public CompletedLoanDetailResponse findCompletedLoanDetail(Long userId, Long applicationId) {
        // product fetch join으로 추가 쿼리 방지
        LoanApplication application = loanApplicationRepository
                .findCompletedDetailByApplicationIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        if (!COMPLETED_STATUSES.contains(application.getStatus())) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        // loan_decision에서 최종 결정(행원 거절 / 지점장 승인 / 지점장 거절) 조회
        LoanDecision decision = loanDecisionRepository
                .findByApplication_ApplicationIdAndStatusIn(applicationId,
                        List.of(DecisionStatus.MANAGER_APPROVED, DecisionStatus.MANAGER_REJECTED, DecisionStatus.TELLER_REJECTED))
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        return LoanConverter.toCompletedDetailResponse(application, decision);
    }
}
