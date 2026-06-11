package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.client.NotificationPushClient;
import com.sofit.admin.domain.loan.converter.LoanDecisionConverter;
import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionErrorCode;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.audit.AuditLog;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.notification.Notification;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanDecisionServiceImpl implements LoanDecisionService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPushClient notificationPushClient;
    private final AdminRoleService adminRoleService;

    @Override
    @AuditLog(action = "LOAN_APPROVE", target = "대출 승인 심사")
    public LoanDecisionResponse approveLoanApplication(Long applicationId, LoanApproveRequest request) {
        // 1. 대출 신청 건 조회
        LoanApplication application = findApplicationOrThrow(applicationId);

        // 2. 이미 결정된 건인지 검증
        validateNotAlreadyDecided(application);

        // 3. 현재 사용자 정보
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserRole currentRole = adminRoleService.getCurrentUserRole();

        // 4. 배정 검증 (지점장은 배정 무관하게 처리 가능)
        if (currentRole != UserRole.ADMIN_BANK_MANAGER) {
            validateAssignment(application, currentUserId);
        }

        // 5. 상태 + 권한 조합 검증
        validateDecisionAuthority(application);

        // 5-1. 시스템 거절 건은 승인 불가
        if (application.getStatus() == ApplicationStatus.SYSTEM_REJECTED) {
            throw new BaseException(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }

        // 6. LoanDecision 생성 및 저장 (createdBy에 현재 userId 명시적 설정)
        DecisionStatus decisionType = (currentRole == UserRole.ADMIN_BANK_TELLER)
                ? DecisionStatus.TELLER_APPROVED
                : DecisionStatus.MANAGER_APPROVED;

        LoanDecision loanDecision = LoanDecision.createApproval(
                application,
                decisionType,
                request.getApprovedAmount(),
                request.getApprovedRate(),
                request.getApprovedTerm(),
                request.getRepaymentMethod(),
                request.getComment(),
                currentUserId
        );
        loanDecisionRepository.save(loanDecision);
        log.info("대출 승인 applicationId={} decision={}", applicationId, decisionType);

        // 7. 역할에 따라 상태 변경 및 알림 분기
        if (currentRole == UserRole.ADMIN_BANK_TELLER) {
            // TELLER: 지점장 리뷰 단계로 전환, 알림 없음
            application.updateStatus(ApplicationStatus.MANAGER_REVIEW);
        } else if (currentRole == UserRole.ADMIN_BANK_MANAGER) {
            // MANAGER: 최종 승인, 알림 생성
            application.updateStatus(ApplicationStatus.APPROVED);
            sendDecisionNotification(application);
        }

        return LoanDecisionConverter.toLoanDecisionResponse(loanDecision);
    }

    @Override
    @AuditLog(action = "LOAN_REJECT", target = "대출 반려 심사")
    public LoanDecisionResponse rejectLoanApplication(Long applicationId, LoanRejectRequest request) {
        // 1. 대출 신청 건 조회
        LoanApplication application = findApplicationOrThrow(applicationId);

        // 2. 이미 결정된 건인지 검증
        validateNotAlreadyDecided(application);

        // 3. 현재 사용자 정보
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserRole currentRole = adminRoleService.getCurrentUserRole();

        // 4. 배정 검증 (지점장은 배정 무관하게 처리 가능)
        if (currentRole != UserRole.ADMIN_BANK_MANAGER) {
            validateAssignment(application, currentUserId);
        }

        // 5. 상태 + 권한 조합 검증
        validateDecisionAuthority(application);

        // 6. LoanDecision 생성 및 저장 (createdBy에 현재 userId 명시적 설정)
        DecisionStatus decisionType = (currentRole == UserRole.ADMIN_BANK_TELLER)
                ? DecisionStatus.TELLER_REJECTED
                : DecisionStatus.MANAGER_REJECTED;

        LoanDecision loanDecision = LoanDecision.createRejection(
                application,
                decisionType,
                request.getComment(),
                currentUserId
        );
        loanDecisionRepository.save(loanDecision);
        log.info("대출 반려 applicationId={} decision={}", applicationId, decisionType);

        // 7. 거절은 행원/지점장 무관하게 최종 → REJECTED, 알림 생성
        application.updateStatus(ApplicationStatus.REJECTED);
        sendDecisionNotification(application);

        return LoanDecisionConverter.toLoanDecisionResponse(loanDecision);
    }

    /**
     * 심사 완료 알림을 DB에 저장하고 sofit-user에 SSE 푸시를 요청한다.
     * 푸시 실패 시에도 심사 처리는 롤백되지 않는다 (NotificationPushClient 내부에서 예외 흡수).
     */
    private void sendDecisionNotification(LoanApplication application) {
        Notification notification = Notification.builder()
                .user(application.getUser())
                .type(NotificationType.LOAN_DECIDED)
                .referenceId(application.getApplicationId())
                .referenceLabel(application.getProduct().getProductName())
                .build();
        notificationRepository.save(notification);

        notificationPushClient.pushNotification(
                NotificationPushRequest.from(notification)
        );
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
     * - SYSTEM_APPROVED / SYSTEM_REJECTED → ADMIN_BANK_TELLER만 처리 가능
     * - MANAGER_REVIEW → ADMIN_BANK_MANAGER만 처리 가능
     */
    private void validateDecisionAuthority(LoanApplication application) {
        ApplicationStatus status = application.getStatus();

        if (status == ApplicationStatus.SYSTEM_APPROVED || status == ApplicationStatus.SYSTEM_REJECTED) {
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

    /**
     * 본인에게 배정된 신청 건인지 검증
     */
    private void validateAssignment(LoanApplication application, Long currentUserId) {
        if (application.getAssignedBankerId() == null ||
                !application.getAssignedBankerId().equals(currentUserId)) {
            throw new BaseException(LoanDecisionErrorCode.NOT_ASSIGNED_TO_ME);
        }
    }
}
