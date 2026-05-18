package com.sofit.user.domain.loan.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanDecisionRepository;
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
            ApplicationStatus.SCB_CALCULATING,
            ApplicationStatus.FINAL_REVIEW
    );

    // 심사 완료 상태 목록
    private static final List<ApplicationStatus> COMPLETED_STATUSES = List.of(
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED
    );

    @Override
    public LoanApplicationListResponse findUnderReviewLoans(Long userId) {
        List<LoanApplicationListResponse.LoanApplicationItem> items = loanApplicationRepository
                .findByUser_IdAndStatusIn(userId, UNDER_REVIEW_STATUSES)
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
                .findByApplicationIdAndUser_Id(applicationId, userId)
                .map(LoanConverter::toDetailResponse)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));
    }

    @Override
    public CompletedLoanListResponse findCompletedLoans(Long userId) {
        List<CompletedLoanListResponse.CompletedLoanItem> items = loanApplicationRepository
                .findByUser_IdAndStatusInOrderByUpdatedAtDesc(userId, COMPLETED_STATUSES)
                .stream()
                .map(LoanConverter::toCompletedListItem)
                .toList();

        return new CompletedLoanListResponse(items);
    }

    @Override
    public CompletedLoanDetailResponse findCompletedLoanDetail(Long userId, Long applicationId) {
        LoanApplication application = loanApplicationRepository
                .findByApplicationIdAndUser_Id(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        if (!COMPLETED_STATUSES.contains(application.getStatus())) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        LoanDecision decision = loanDecisionRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        return LoanConverter.toCompletedDetailResponse(application, decision);
    }
}
