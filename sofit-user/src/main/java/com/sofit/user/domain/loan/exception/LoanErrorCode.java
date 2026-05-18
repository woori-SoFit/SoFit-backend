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
    EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4044", "실행 건을 찾을 수 없습니다."),

    // 계좌 인증 에러 코드
    ACCOUNT_INVALID(HttpStatus.BAD_REQUEST, "ACCOUNT4001", "유효하지 않은 계좌번호입니다."),
    ACCOUNT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT4002", "일일 요청 한도(5회)를 초과했습니다."),
    ACCOUNT_VERIFICATION_MISMATCH(HttpStatus.BAD_REQUEST, "ACCOUNT4003", "인증번호가 일치하지 않습니다."),
    ACCOUNT_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "ACCOUNT4004", "인증 시간이 만료되었습니다. 다시 요청해주세요."),
    ACCOUNT_INVALID_BANK_CODE(HttpStatus.BAD_REQUEST, "ACCOUNT4005", "유효하지 않은 은행코드입니다."),
    ACCOUNT_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "ACCOUNT5001", "계좌 인증 서비스에 일시적인 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
