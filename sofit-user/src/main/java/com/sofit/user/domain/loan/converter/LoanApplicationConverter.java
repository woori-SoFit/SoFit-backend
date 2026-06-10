package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftItemResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;

import java.util.List;

/**
 * LoanApplication 엔티티 ↔ 대출 신청 관련 DTO 변환 클래스
 */
public class LoanApplicationConverter {

    private LoanApplicationConverter() {
    }

    /**
     * 대출 신청 생성 후 응답 변환
     * Entity → LoanApplicationCreateResponse (applicationId만 반환)
     */
    public static LoanApplicationCreateResponse toCreateResponse(LoanApplication application) {
        return new LoanApplicationCreateResponse(application.getApplicationId());
    }

    /**
     * DRAFT 존재 여부 확인 응답 변환
     * Entity → DraftCheckResponse (hasDraft=true, resumeStep 계산 포함)
     */
    public static DraftCheckResponse toDraftCheckResponse(LoanApplication application) {
        LastCompletedStep step = application.getLastCompletedStep();
        String lastStep = step != null ? step.name() : null;
        String resumeStep = LastCompletedStep.getResumeStep(step);

        return new DraftCheckResponse(true, application.getApplicationId(), lastStep, resumeStep);
    }

    /**
     * 이어가기 데이터 조회 응답 변환
     * Entity → LoanApplicationResumeResponse (저장된 입력값 + resumeStep)
     */
    public static LoanApplicationResumeResponse toResumeResponse(LoanApplication application) {
        LastCompletedStep step = application.getLastCompletedStep();
        String resumeStep = LastCompletedStep.getResumeStep(step);

        LoanApplicationResumeResponse.SavedData savedData = new LoanApplicationResumeResponse.SavedData(
                application.getUserInputAnnualIncome() != null
                        ? application.getUserInputAnnualIncome() : null,
                application.getUserInputCreditScore() != null
                        ? application.getUserInputCreditScore(): null,
                application.getUserInputIncomeType() != null
                        ? application.getUserInputIncomeType().getCode() : null,
                application.getUserInputExistingLoanAmt() != null
                        ? application.getUserInputExistingLoanAmt(): null,
                step != null && step.ordinal() >= LastCompletedStep.CONSENT_DONE.ordinal()
        );

        return new LoanApplicationResumeResponse(
                application.getApplicationId(),
                resumeStep,
                savedData
        );
    }

    /**
     * 최종 제출(심사 요청) 후 응답 변환
     * Entity → LoanApplicationSubmitResponse
     */
    public static LoanApplicationSubmitResponse toSubmitResponse(LoanApplication application) {
        return new LoanApplicationSubmitResponse(
                application.getApplicationId(),
                application.getProduct().getProductName(),
                application.getRequestedAmount(),
                application.getAppliedAt(),
                application.getRepaymentMethod().name(),
                application.getPurpose().name(),
                application.getRequestedTerm()
        );
    }

    /**
     * 사용자의 전체 DRAFT 목록 응답 변환
     * List<Entity> → DraftListResponse
     */
    public static DraftListResponse toDraftListResponse(List<LoanApplication> applications) {
        List<DraftItemResponse> items = applications.stream()
                .map(app -> {
                    LastCompletedStep step = app.getLastCompletedStep();
                    String lastStep = step != null ? step.name() : null;
                    String resumeStep = LastCompletedStep.getResumeStep(step);
                    return new DraftItemResponse(
                            app.getApplicationId(),
                            app.getProduct().getProductId(),
                            app.getProduct().getProductName(),
                            lastStep,
                            resumeStep
                    );
                })
                .toList();
        return new DraftListResponse(items);
    }
}
