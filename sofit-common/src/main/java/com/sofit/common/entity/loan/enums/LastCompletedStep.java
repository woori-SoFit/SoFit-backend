package com.sofit.common.entity.loan.enums;

public enum LastCompletedStep {
    CONSENT_DONE,      // 약관 동의 완료
    AUTH_DONE,         // 본인인증(금융인증서) 완료
    BIZ_INFO_DONE,     // 사업자 정보 확인 완료
    DATA_COLLECTED,    // 마이데이터 수집 완료
    MYBIZ_CONNECTED    // 마이비즈데이터 연동 완료
}
