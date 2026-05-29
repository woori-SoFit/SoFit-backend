package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanDecisionSuccessCode implements BaseSuccessCode {

    LOAN_APPROVE_OK(HttpStatus.OK, "LOAN_ADMIN2001", "대출 승인 처리에 성공했습니다."),
    LOAN_REJECT_OK(HttpStatus.OK, "LOAN_ADMIN2002", "대출 거절 처리에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
