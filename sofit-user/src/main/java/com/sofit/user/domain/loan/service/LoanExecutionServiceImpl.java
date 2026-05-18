package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.repository.LoanDecisionRepository;
import com.sofit.common.repository.LoanExecutionRepository;
import com.sofit.user.domain.loan.converter.LoanExecutionConverter;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanExecutionServiceImpl implements LoanExecutionService {

    private final LoanExecutionRepository loanExecutionRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    @Override
    public LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId) {
        LoanExecution execution = loanExecutionRepository
                .findByApplicationIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        LoanDecision decision = loanDecisionRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        return LoanExecutionConverter.toResponse(execution, decision);
    }
}
