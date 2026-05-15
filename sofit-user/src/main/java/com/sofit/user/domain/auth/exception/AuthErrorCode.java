package com.sofit.user.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // KYC 관련
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4004", "일치하는 사업자등록번호를 찾을 수 없습니다."),

    // PIN 관련
    PIN_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH4001", "PIN 번호가 올바르지 않습니다."),
    CERT_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4005", "등록된 금융인증서를 찾을 수 없습니다."),

    // 공통
    INVALID_INPUT_FORMAT(HttpStatus.BAD_REQUEST, "AUTH4006", "입력값 형식이 올바르지 않습니다."),
    EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH5001", "외부 인증 서버와 통신 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
