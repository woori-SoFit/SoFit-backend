package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanProductRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.loan.converter.LoanApplicationConverter;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.notification.event.LoanSubmittedEvent;
import com.sofit.common.audit.AuditLog;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanProductRepository loanProductRepository;
    private final UserRepository userRepository;
    private final BankerAssignmentService bankerAssignmentService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 대출 신청 생성 (DRAFT 상태)
     * - 1차 필터링은 프론트에서 완료된 상태이므로 백엔드에서는 비교 로직 없음
     * - 요청값을 그대로 loan_application 레코드에 DRAFT 상태로 저장
     */
    @Override
    @Transactional
    @AuditLog(action = "LOAN_APPLICATION_CREATE", target = "대출 신청 생성")
    public LoanApplicationCreateResponse createApplication(Long userId, Long productId,
                                                           LoanApplicationCreateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        // 2. 상품 존재 + ACTIVE 확인
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BaseException(LoanErrorCode.PRODUCT_NOT_ACTIVE);
        }

        // 3. 동일 상품 중복 신청 체크 (EXECUTED, REJECTED, CANCELLED, EXPIRED 상태는 재신청 허용)
        boolean exists = loanApplicationRepository
                .existsByUser_UserIdAndProduct_ProductIdAndStatusNotIn(
                        userId, productId,
                        List.of(ApplicationStatus.EXECUTED, ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED, ApplicationStatus.EXPIRED));

        if (exists) {
            throw new BaseException(LoanErrorCode.DUPLICATE_APPLICATION);
        }

        // 4. DRAFT 생성 & 저장
        LoanApplication application = LoanApplication.createDraft(
                user, product,
                request.getAnnualIncome(),
                request.getCreditScore(),
                request.getIncomeType(),
                request.getExistingLoanAmt()
        );

        loanApplicationRepository.save(application);
        log.info("대출 신청 DRAFT 생성 applicationId={} productId={}", application.getApplicationId(), productId);

        return LoanApplicationConverter.toCreateResponse(application);
    }

    /**
     * DRAFT 존재 여부 확인
     * - 특정 상품에 대해 현재 사용자의 DRAFT 상태 신청이 있는지 조회
     * - 존재하면 hasDraft=true + resumeStep 반환, 없으면 hasDraft=false
     */
    @Override
    public DraftCheckResponse checkDraft(Long userId, Long productId) {
        return loanApplicationRepository
                .findByUser_UserIdAndProduct_ProductIdAndStatus(userId, productId, ApplicationStatus.DRAFT)
                .map(LoanApplicationConverter::toDraftCheckResponse)
                .orElse(new DraftCheckResponse(false, null, null, null));
    }

    /**
     * 사용자의 전체 DRAFT 목록 조회
     * - 로그인 사용자의 모든 DRAFT 상태 신청을 상품명 포함하여 반환
     */
    @Override
    public DraftListResponse findDrafts(Long userId) {
        java.util.List<LoanApplication> drafts = loanApplicationRepository
                .findDraftsByUserIdWithProduct(userId, ApplicationStatus.DRAFT);
        return LoanApplicationConverter.toDraftListResponse(drafts);
    }

    /**
     * 이어가기 데이터 조회
     * - DRAFT 상태인 신청의 저장된 데이터를 반환하여 프론트에서 화면 복원에 사용
     * - DRAFT가 아닌 상태의 신청은 조회 불가
     */
    @Override
    public LoanApplicationResumeResponse getResumeData(Long userId, Long applicationId) {
        LoanApplication application = loanApplicationRepository
                .findByApplicationIdAndUser_UserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        return LoanApplicationConverter.toResumeResponse(application);
    }

    /**
     * 최종 제출 (심사 요청)
     * - DRAFT 상태인 신청을 SUBMITTED로 변경하고 applied_at을 기록
     * - 희망 대출 조건(금액, 기간, 상환방식, 용도)을 저장
     */
    @Override
    @Transactional
    @AuditLog(action = "LOAN_APPLICATION_SUBMIT", target = "대출 신청 제출")
    public LoanApplicationSubmitResponse submitApplication(Long userId, Long applicationId,
                                                           LoanApplicationSubmitRequest request) {
        // 1. 본인 소유 확인
        LoanApplication application = loanApplicationRepository
                .findByApplicationIdAndUser_UserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        // 2. DRAFT 상태 확인
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_DRAFT);
        }

        // 3. 담당 은행원 배정 (실패 시 예외 → 트랜잭션 롤백)
        Long bankerId = bankerAssignmentService.assignBanker();
        application.assignBanker(bankerId);

        // 4. 제출 처리 (status → SUBMITTED, appliedAt 기록)
        application.submit(
                request.getRequestedAmount(),
                request.getRequestedTerm(),
                request.getRepaymentMethod(),
                request.getPurpose()
        );

        log.info("대출 신청 제출 applicationId={} bankerId={}", applicationId, bankerId);

        // 5. 대출 신청 완료 알림 이벤트 발행 (트랜잭션 커밋 후 처리)
        // AFTER_COMMIT 이후 영속 컨텍스트가 닫히므로 엔티티 대신 ID만 전달
        eventPublisher.publishEvent(new LoanSubmittedEvent(
                application.getUser().getUserId(),
                application.getApplicationId()
        ));

        return LoanApplicationConverter.toSubmitResponse(application);
    }

    /**
     * DRAFT 신청서 취소 (소프트 삭제)
     * - 존재 여부 → 본인 소유 → DRAFT 상태 순서로 검증
     * - 검증 통과 시 status를 CANCELLED로 변경 (실제 row 삭제 없음)
     */
    @Override
    @Transactional
    @AuditLog(action = "LOAN_APPLICATION_CANCEL", target = "대출 신청 취소")
    public void cancelDraftApplication(Long userId, Long applicationId) {
        // 1. 존재 여부 검증
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        // 2. 본인 소유 검증
        if (!application.getUser().getUserId().equals(userId)) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED);
        }

        // 3. DRAFT 상태 검증
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_DRAFT);
        }

        // 4. 소프트 삭제 (status → CANCELLED)
        application.updateStatus(ApplicationStatus.CANCELLED);
        log.info("대출 신청 취소 applicationId={}", applicationId);
    }
}
