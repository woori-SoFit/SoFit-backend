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
    APPLICATION_NOT_APPROVED(HttpStatus.BAD_REQUEST, "LOAN4005", "승인된 대출 신청만 계좌 인증을 진행할 수 있습니다."),
    STEP_ORDER_VIOLATION(HttpStatus.BAD_REQUEST, "LOAN4003", "이전 단계를 먼저 완료해야 합니다."),
    REQUIRED_CONSENT_MISSING(HttpStatus.BAD_REQUEST, "LOAN4004", "필수 약관에 모두 동의해야 합니다."),
    INVALID_BIZ_NO_FORMAT(HttpStatus.BAD_REQUEST, "LOAN4006", "사업자등록번호는 10자리 숫자여야 합니다."),
    APPLICATION_NOT_OWNED(HttpStatus.FORBIDDEN, "LOAN4032", "본인의 대출 신청만 처리할 수 있습니다."),
    BIZ_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4045", "해당 사업자등록번호로 사업자 정보를 찾을 수 없습니다."),
    MYBIZ_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4046", "마이비즈 데이터를 찾을 수 없습니다."),
    NO_AVAILABLE_BANKER(HttpStatus.INTERNAL_SERVER_ERROR, "LOAN5001", "배정 가능한 은행원이 없습니다."),
    EXTERNAL_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LOAN5002", "외부 서비스 호출에 실패했습니다."),

    // 계좌 인증 에러 코드
    ACCOUNT_INVALID(HttpStatus.BAD_REQUEST, "ACCOUNT4001", "유효하지 않은 계좌번호입니다."),
    ACCOUNT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT4002", "일일 요청 한도(5회)를 초과했습니다."),
    ACCOUNT_VERIFICATION_MISMATCH(HttpStatus.BAD_REQUEST, "ACCOUNT4003", "인증번호가 일치하지 않습니다."),
    ACCOUNT_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "ACCOUNT4004", "인증 시간이 만료되었습니다. 다시 요청해주세요."),
    ACCOUNT_INVALID_BANK_CODE(HttpStatus.BAD_REQUEST, "ACCOUNT4005", "유효하지 않은 은행코드입니다."),
    EXECUTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACCOUNT4091", "이미 대출 실행이 완료된 건입니다."),
    ACCOUNT_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "ACCOUNT5001", "계좌 인증 서비스에 일시적인 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
