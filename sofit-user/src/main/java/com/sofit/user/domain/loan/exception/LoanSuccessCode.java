package com.sofit.user.domain.loan.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoanSuccessCode implements BaseSuccessCode {

    LOAN_PRODUCT_LIST_OK(HttpStatus.OK, "LOAN2001", "대출 상품 목록 조회에 성공했습니다."),
    LOAN_PRODUCT_DETAIL_OK(HttpStatus.OK, "LOAN2002", "대출 상품 상세 조회에 성공했습니다."),
    LOAN_APPLICATION_CREATED(HttpStatus.OK, "LOAN2003", "대출 신청이 생성되었습니다."),
    LOAN_DRAFT_CHECK_OK(HttpStatus.OK, "LOAN2004", "DRAFT 조회에 성공했습니다."),
    LOAN_SUBMIT_OK(HttpStatus.OK, "LOAN2005", "대출 심사 요청에 성공했습니다."),
    LOAN_APPLICATION_LIST_OK(HttpStatus.OK, "LOAN2006", "심사 중인 대출 목록 조회에 성공했습니다."),
    LOAN_APPLICATION_DETAIL_OK(HttpStatus.OK, "LOAN2007", "심사 중인 대출 상세 조회에 성공했습니다."),
    LOAN_APPLICATION_COMPLETED_LIST_OK(HttpStatus.OK, "LOAN2008", "심사 완료 대출 목록 조회에 성공했습니다."),
    LOAN_APPLICATION_COMPLETED_DETAIL_OK(HttpStatus.OK, "LOAN2009", "심사 완료 대출 상세 조회에 성공했습니다."),
    LOAN_EXECUTION_RESULT_OK(HttpStatus.OK, "LOAN2010", "대출 실행 결과 조회에 성공했습니다."),
    LOAN_RESUME_OK(HttpStatus.OK, "LOAN2011", "이어가기 데이터 조회에 성공했습니다."),
    LOAN_PRODUCT_OPTIONS_OK(HttpStatus.OK, "LOAN2012", "대출 상품 옵션 조회에 성공했습니다."),
    LOAN_STEP_CONSENT_OK(HttpStatus.OK, "LOAN2013", "대출 약관 동의가 완료되었습니다."),
    LOAN_STEP_BIZ_INFO_OK(HttpStatus.OK, "LOAN2014", "사업자 정보 확인이 완료되었습니다."),
    LOAN_STEP_MYDATA_OK(HttpStatus.OK, "LOAN2015", "마이데이터 약관 동의가 완료되었습니다."),
    LOAN_STEP_MYBIZ_OK(HttpStatus.OK, "LOAN2016", "마이비즈데이터 연동이 완료되었습니다."),
    LOAN_DRAFT_CANCELLED(HttpStatus.OK, "LOAN2017", "대출 신청서가 삭제되었습니다."),
    LOAN_DRAFT_LIST_OK(HttpStatus.OK, "LOAN2018", "진행 중인 DRAFT 목록 조회에 성공했습니다."),

    // 계좌 인증 성공 코드
    ACCOUNT_VERIFICATION_OK(HttpStatus.OK, "ACCOUNT2011", "1원 송금 요청에 성공했습니다."),
    ACCOUNT_VERIFICATION_CONFIRM_OK(HttpStatus.OK, "ACCOUNT2012", "계좌 인증에 성공했습니다."),

    // 대출 실행 목록 조회
    LOAN_EXECUTION_LIST_OK(HttpStatus.OK, "LOAN2019", "대출 실행 완료 목록 조회에 성공했습니다.");
    
    

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
