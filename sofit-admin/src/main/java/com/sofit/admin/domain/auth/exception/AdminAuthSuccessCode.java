package com.sofit.admin.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminAuthSuccessCode implements BaseSuccessCode {

    LOGIN_SUCCESS(HttpStatus.OK, "AUTH2001", "로그인에 성공했습니다."),
    ME_SUCCESS(HttpStatus.OK, "ADMIN2001", "관리자 정보 조회에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH2004", "로그아웃에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
