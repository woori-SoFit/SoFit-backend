package com.sofit.user.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanSuccessCode implements BaseSuccessCode {

    LOAN_PRODUCT_LIST_OK(HttpStatus.OK, "LOAN2001", "대출 상품 목록 조회에 성공했습니다."),
    LOAN_PRODUCT_DETAIL_OK(HttpStatus.OK, "LOAN2002", "대출 상품 상세 조회에 성공했습니다."),
    ELIGIBILITY_CHECK_OK(HttpStatus.OK, "LOAN2003", "신청 가능 여부 확인에 성공했습니다."),
    ELIGIBILITY_CHECK_FAIL(HttpStatus.OK, "LOAN2004", "대출 신청 조건에 부합하지 않습니다."),
    LOAN_SUBMIT_OK(HttpStatus.OK, "LOAN2005", "대출 심사 요청에 성공했습니다."),
    LOAN_APPLICATION_LIST_OK(HttpStatus.OK, "LOAN2006", "심사 중인 대출 목록 조회에 성공했습니다."),
    LOAN_APPLICATION_DETAIL_OK(HttpStatus.OK, "LOAN2007", "심사 중인 대출 상세 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
