package com.sofit.user.domain.terms.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermSuccessCode implements BaseSuccessCode {

    TERM_LIST_OK(HttpStatus.OK, "TERM2000", "약관 목록 조회에 성공했습니다."),
    CONSENT_OK(HttpStatus.OK, "TERM2001", "약관 동의가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
