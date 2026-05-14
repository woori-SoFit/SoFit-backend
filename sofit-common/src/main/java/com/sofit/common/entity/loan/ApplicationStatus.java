package com.sofit.common.entity.loan;

public enum ApplicationStatus {
    DRAFT,             // 초안 (1차 필터링 확인 시 생성)
    SUBMITTED,         // 심사 요청 완료
    CB_CHECKING,       // CB 점수 조회 중
    BASIC_REVIEW,      // 기본 심사
    SCB_CALCULATING,   // SCB 점수 산출 중
    FINAL_REVIEW,      // 최종 심사
    APPROVED,          // 승인
    REJECTED,          // 거절
    CONTRACTED,        // 약정 체결
    EXECUTED,          // 대출 실행
    CANCELLED          // 취소
}
