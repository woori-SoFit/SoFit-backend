package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;

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
        String resumeStep = calculateResumeStep(step);

        return new DraftCheckResponse(true, application.getApplicationId(), lastStep, resumeStep);
    }

    /**
     * 이어가기 데이터 조회 응답 변환
     * Entity → LoanApplicationResumeResponse (저장된 입력값 + resumeStep)
     */
    public static LoanApplicationResumeResponse toResumeResponse(LoanApplication application) {
        LastCompletedStep step = application.getLastCompletedStep();
        String resumeStep = calculateResumeStep(step);

        LoanApplicationResumeResponse.SavedData savedData = new LoanApplicationResumeResponse.SavedData(
                application.getUserInputAnnualIncome() != null
                        ? application.getUserInputAnnualIncome().name() : null,
                application.getUserInputCreditScore() != null
                        ? application.getUserInputCreditScore().name() : null,
                application.getUserInputIncomeType() != null
                        ? application.getUserInputIncomeType().getCode() : null,
                application.getUserInputExistingLoanAmt() != null
                        ? application.getUserInputExistingLoanAmt().name() : null,
                step != null && step.ordinal() >= LastCompletedStep.CONSENT_DONE.ordinal()
        );

        return new LoanApplicationResumeResponse(
                application.getApplicationId(),
                resumeStep,
                savedData
        );
    }

    /**
     * lastCompletedStep 기반으로 다음 진행할 단계(resumeStep)를 계산한다.
     * - null: Step 1만 완료 → 약관 동의(CONSENT)부터
     * - CONSENT_DONE → 본인인증(AUTH)
     * - AUTH_DONE → 사업자 정보 확인(BIZ_INFO)
     * - BIZ_INFO_DONE → 마이데이터 수집(COLLECT_DATA)
     * - DATA_COLLECTED → 마이비즈데이터 연동(MYBIZ)
     * - MYBIZ_CONNECTED → 최종 제출(SUBMIT)
     */
    private static String calculateResumeStep(LastCompletedStep step) {
        if (step == null) {
            return "CONSENT";
        }
        return switch (step) {
            case CONSENT_DONE -> "AUTH";
            case AUTH_DONE -> "BIZ_INFO";
            case BIZ_INFO_DONE -> "COLLECT_DATA";
            case DATA_COLLECTED -> "MYBIZ";
            case MYBIZ_CONNECTED -> "SUBMIT";
        };
    }
}
