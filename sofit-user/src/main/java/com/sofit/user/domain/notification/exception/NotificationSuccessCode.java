package com.sofit.user.domain.notification.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationSuccessCode implements BaseSuccessCode {

    UNREAD_LIST_OK(HttpStatus.OK, "NOTI2000", "미읽음 알림 조회에 성공했습니다."),
    MARK_READ_OK(HttpStatus.OK, "NOTI2001", "알림 읽음 처리에 성공했습니다."),
    PUSH_OK(HttpStatus.OK, "NOTI2002", "알림 푸시에 성공했습니다."),
    NOTIFICATION_LIST_OK(HttpStatus.OK, "NOTI2003", "알림 목록 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
