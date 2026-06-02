package com.sofit.common.entity.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    LOAN_SUBMITTED("대출 신청 완료", "대출 신청이 정상적으로 접수되었습니다"),
    LOAN_DECIDED("대출 심사 완료", "신청하신 대출 심사가 완료되었습니다"),
    LOAN_EXECUTED("대출 실행 완료", "대출이 정상적으로 실행되었습니다");

    private final String title;
    private final String message;
}
