package com.sofit.user.domain.loan.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.user.domain.loan.converter.LoanConverter;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.repository.LoanApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanApplicationRepository loanApplicationRepository;

    // 심사 중 상태 목록
    private static final List<ApplicationStatus> UNDER_REVIEW_STATUSES = List.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.CB_CHECKING,
            ApplicationStatus.BASIC_REVIEW,
            ApplicationStatus.SCB_CALCULATING,
            ApplicationStatus.FINAL_REVIEW
    );

    @Override
    public List<LoanApplicationListResponse> findUnderReviewLoans(Long userId) {
        return loanApplicationRepository
                .findByUser_IdAndStatusIn(userId, UNDER_REVIEW_STATUSES)
                .stream()
                .map(LoanConverter::toListResponse)
                .toList();
    }

    @Override
    public LoanApplicationDetailResponse findLoanDetail(Long userId, Long applicationId) {
        return loanApplicationRepository
                .findByApplicationIdAndUser_Id(applicationId, userId)
                .map(LoanConverter::toDetailResponse)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_APPLICATION_NOT_FOUND));
    }
}
