package com.sofit.user.domain.terms.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermErrorCode implements BaseErrorCode {

    // 400 Bad Request
    TERM_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "TERM4001", "약관 유형이 일치하지 않습니다."),
    REQUIRED_TERM_NOT_CONSENTED(HttpStatus.BAD_REQUEST, "TERM4002", "필수 약관에 동의하지 않았습니다."),
    ALREADY_CONSENTED(HttpStatus.BAD_REQUEST, "TERM4003", "이미 동의한 약관입니다."),

    // 404 Not Found
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM4041", "존재하지 않는 약관입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
