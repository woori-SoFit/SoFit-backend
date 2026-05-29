package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanDecisionConverter;
import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionErrorCode;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanDecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanDecisionServiceImpl implements LoanDecisionService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    @Override
    public LoanDecisionResponse approveLoanApplication(Long applicationId, LoanApproveRequest request) {
        // 1. 대출 신청 건 조회
        LoanApplication application = findApplicationOrThrow(applicationId);

        // 2. 이미 결정된 건인지 검증
        validateNotAlreadyDecided(application);

        // 3. 상태 + 권한 조합 검증
        validateDecisionAuthority(application);

        // 4. LoanDecision 생성 및 저장 (created_by는 BaseEntity의 @CreatedBy로 자동 설정)
        LoanDecision loanDecision = LoanDecision.createApproval(
                application,
                request.getApprovedAmount(),
                request.getApprovedRate(),
                request.getApprovedTerm(),
                request.getRepaymentMethod(),
                request.getComment()
        );
        loanDecisionRepository.save(loanDecision);

        // 5. 대출 신청 상태 변경
        application.updateStatus(ApplicationStatus.APPROVED);

        return LoanDecisionConverter.toLoanDecisionResponse(loanDecision);
    }

    @Override
    public LoanDecisionResponse rejectLoanApplication(Long applicationId, LoanRejectRequest request) {
        // 1. 대출 신청 건 조회
        LoanApplication application = findApplicationOrThrow(applicationId);

        // 2. 이미 결정된 건인지 검증
        validateNotAlreadyDecided(application);

        // 3. 상태 + 권한 조합 검증
        validateDecisionAuthority(application);

        // 4. LoanDecision 생성 및 저장 (created_by는 BaseEntity의 @CreatedBy로 자동 설정)
        LoanDecision loanDecision = LoanDecision.createRejection(
                application,
                request.getComment()
        );
        loanDecisionRepository.save(loanDecision);

        // 5. 대출 신청 상태 변경
        application.updateStatus(ApplicationStatus.REJECTED);

        return LoanDecisionConverter.toLoanDecisionResponse(loanDecision);
    }

    private LoanApplication findApplicationOrThrow(Long applicationId) {
        return loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanDecisionErrorCode.APPLICATION_NOT_FOUND));
    }

    private void validateNotAlreadyDecided(LoanApplication application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            throw new BaseException(LoanDecisionErrorCode.ALREADY_DECIDED);
        }
    }

    /**
     * 상태 + 권한 조합 검증
     * - SYSTEM_APPROVED → ADMIN_BANK_TELLER만 처리 가능
     * - MANAGER_REVIEW → ADMIN_BANK_MANAGER만 처리 가능
     */
    private void validateDecisionAuthority(LoanApplication application) {
        ApplicationStatus status = application.getStatus();

        if (status == ApplicationStatus.SYSTEM_APPROVED) {
            if (!SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")) {
                throw new BaseException(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
            }
        } else if (status == ApplicationStatus.MANAGER_REVIEW) {
            if (!SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")) {
                throw new BaseException(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
            }
        } else {
            throw new BaseException(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }
    }
}
