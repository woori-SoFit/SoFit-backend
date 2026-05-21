package com.sofit.admin.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminAuthErrorCode implements BaseErrorCode {

    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4001", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
