package com.sofit.common.entity.loan.enums;

public enum ApplicationStatus {
    DRAFT,             // 초안 (1차 필터링 확인 시 생성)
    SUBMITTED,         // 심사 요청 완료
    CB_CHECKING,       // CB 점수 조회 중
    BASIC_REVIEW,      // 기본 심사
    S_CALCULATING,     // 성장 S등급 산출 중
    S_COMPLETED,       // 성장 S등급 산출 완료 
    SYSTEM_APPROVED,   // 시스템 승인
    SYSTEM_REJECTED,       // 시스템 거절
    MANAGER_REVIEW,    // 지점장 리뷰를 기다리는 중 
    APPROVED,          // 승인
    REJECTED,          // 거절
    CONTRACTED,        // 약정 체결
    EXECUTED,          // 대출 실행
    CANCELLED,         // 대출 신청 취소
    EXPIRED            // DRAFT 만료 (7일 경과)
}
