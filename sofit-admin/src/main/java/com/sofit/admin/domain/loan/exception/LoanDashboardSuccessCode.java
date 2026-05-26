package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanDashboardSuccessCode implements BaseSuccessCode {

    LOAN_DASHBOARD_OK(HttpStatus.OK, "LOAN2001", "대출 신청 목록 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
