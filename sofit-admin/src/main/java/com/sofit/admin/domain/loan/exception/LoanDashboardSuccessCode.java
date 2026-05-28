package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanDashboardSuccessCode implements BaseSuccessCode {

    LOAN_DASHBOARD_OK(HttpStatus.OK, "LOAN2001", "대출 신청 목록 조회에 성공했습니다."),
    LOAN_APPLICATION_DETAIL_OK(HttpStatus.OK, "LOAN2002", "대출 신청 상세 조회에 성공했습니다."),
    LOAN_APPLICATION_INFO_OK(HttpStatus.OK, "LOAN2003", "대출 신청 정보 탭 조회에 성공했습니다."),
    MY_BIZ_DATA_DETAIL_OK(HttpStatus.OK, "LOAN2004", "My Biz Data 탭 조회에 성공했습니다."),
    LOAN_APPLICATION_GRADE_OK(HttpStatus.OK, "LOAN2005", "성장 S등급 탭 조회에 성공했습니다."),
    LOAN_APPLICATION_REVIEW_OK(HttpStatus.OK, "LOAN2006", "심사 결과 탭 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
