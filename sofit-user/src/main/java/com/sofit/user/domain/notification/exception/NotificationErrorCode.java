package com.sofit.user.domain.notification.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI4004", "해당 알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTI4003", "해당 알림에 대한 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
