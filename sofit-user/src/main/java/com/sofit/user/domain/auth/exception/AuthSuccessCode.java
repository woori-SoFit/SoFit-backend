package com.sofit.user.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    BUSINESS_VERIFIED(HttpStatus.OK, "AUTH2001", "사업자 인증에 성공했습니다."),
    FINANCIAL_CERT_VERIFIED(HttpStatus.OK, "AUTH2006", "금융인증서 본인인증에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
