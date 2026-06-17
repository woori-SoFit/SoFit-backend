package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanDashboardErrorCode implements BaseErrorCode {

    INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "LOAN4001", "유효하지 않은 심사 상태입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
