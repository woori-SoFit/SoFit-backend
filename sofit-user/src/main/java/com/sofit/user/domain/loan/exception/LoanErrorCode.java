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
    KYC_NOT_COMPLETED(HttpStatus.FORBIDDEN, "LOAN4031", "KYC 인증이 완료되지 않았습니다."),
    DUPLICATE_APPLICATION(HttpStatus.CONFLICT, "LOAN4091", "이미 해당 상품에 대출 신청이 존재합니다."),
    PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "LOAN4001", "현재 신청할 수 없는 상품입니다."),
    APPLICATION_NOT_DRAFT(HttpStatus.BAD_REQUEST, "LOAN4002", "DRAFT 상태가 아닌 신청은 제출할 수 없습니다."),
    NO_AVAILABLE_BANKER(HttpStatus.INTERNAL_SERVER_ERROR, "LOAN5001", "배정 가능한 은행원이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
