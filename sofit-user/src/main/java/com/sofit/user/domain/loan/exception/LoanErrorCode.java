package com.sofit.user.domain.loan.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoanErrorCode implements BaseErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4041", "존재하지 않는 대출 상품입니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4042", "존재하지 않는 대출 신청입니다."),
    LOAN_DECISION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4043", "심사 결정 정보를 찾을 수 없습니다."),
    EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4044", "실행 건을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
