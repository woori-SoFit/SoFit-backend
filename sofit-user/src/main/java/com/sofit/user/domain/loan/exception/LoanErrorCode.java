package com.sofit.user.domain.loan.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoanErrorCode implements BaseErrorCode {

    LOAN_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4041", "대출 신청 건을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
