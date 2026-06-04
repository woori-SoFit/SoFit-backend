package com.sofit.common.entity.loan.enums;

import lombok.Getter;

@Getter
public enum LastCompletedStep {
    CONSENT_DONE("BIZ_INFO"),           // 약관 동의 완료 → 다음: 사업자 정보 확인
    BIZ_INFO_DONE("COLLECT_DATA"),      // 사업자 정보 확인 완료 → 다음: 마이데이터 수집
    DATA_COLLECTED("MYBIZ"),             // 마이데이터 수집 완료 → 다음: 마이비즈데이터 연동
    MYBIZ_CONNECTED("LOAN_CONDITION");   // 마이비즈데이터 연동 완료 → 다음: 대출 조건 입력

    private final String nextStep;

    LastCompletedStep(String nextStep) {
        this.nextStep = nextStep;
    }

    /**
     * lastCompletedStep 기반으로 다음 진행할 단계(resumeStep)를 반환한다.
     * null이면 Step 1만 완료된 상태 → 약관 동의(CONSENT)부터 시작
     */
    public static String getResumeStep(LastCompletedStep step) {
        if (step == null) {
            return "CONSENT";
        }
        return step.getNextStep();
    }
}
