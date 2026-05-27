package com.sofit.common.entity.auth.enums;

/**
 * 회원가입 멀티스텝 플로우 단계 추적용 Enum.
 * 가입 완료 시 레코드 자체를 삭제하므로 COMPLETED 상태는 불필요.
 */
public enum RegistrationStep {
    KYC_VERIFIED,   // Step 1: 사업자등록번호 KYC 인증 완료
    PIN_VERIFIED,   // Step 2: 금융인증서 PIN 인증 완료
    EXPIRED         // 30분 만료
}
