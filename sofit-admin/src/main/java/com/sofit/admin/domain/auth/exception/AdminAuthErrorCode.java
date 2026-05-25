package com.sofit.admin.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminAuthErrorCode implements BaseErrorCode {

    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4001", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "AUTH4291", "로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해 주세요."),
    CONCURRENT_LOGIN(HttpStatus.CONFLICT, "AUTH4091", "이미 다른 기기에서 로그인되어 있습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH4011", "세션이 만료되었습니다. 다시 로그인해 주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4041", "요청한 리소스를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
