package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanProductRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.loan.converter.LoanApplicationConverter;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanProductRepository loanProductRepository;
    private final UserRepository userRepository;

    /**
     * 대출 신청 생성 (DRAFT 상태)
     * - 1차 필터링은 프론트에서 완료된 상태이므로 백엔드에서는 비교 로직 없음
     * - 요청값을 그대로 loan_application 레코드에 DRAFT 상태로 저장
     */
    @Override
    @Transactional
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

        // 3. 동일 상품 중복 신청 체크 (CANCELLED 제외)
        boolean exists = loanApplicationRepository
                .existsByUser_UserIdAndProduct_ProductIdAndStatusNot(
                        userId, productId, ApplicationStatus.CANCELLED);

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

        // 3. 제출 처리 (status → SUBMITTED, appliedAt 기록)
        application.submit(
                request.getRequestedAmount(),
                request.getRequestedTerm(),
                request.getRepaymentMethod(),
                request.getPurpose()
        );

        return LoanApplicationConverter.toSubmitResponse(application);
    }
}
