package com.sofit.user.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanErrorCode implements BaseErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4041", "존재하지 않는 대출 상품입니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4042", "존재하지 않는 대출 신청입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
