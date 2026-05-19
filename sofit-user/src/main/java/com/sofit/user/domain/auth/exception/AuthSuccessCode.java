package com.sofit.user.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    // 회원가입
    BUSINESS_VERIFIED(HttpStatus.OK, "AUTH2001", "사업자등록번호 인증에 성공했습니다."),
    PIN_VERIFIED(HttpStatus.OK, "AUTH2002", "금융인증서 PIN 인증에 성공했습니다."),
    SIGNUP_COMPLETED(HttpStatus.CREATED, "AUTH2003", "회원가입이 완료되었습니다."),

    // 로그인/로그아웃
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH2004", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH2005", "로그아웃되었습니다."),

    // 회원탈퇴
    WITHDRAW_SUCCESS(HttpStatus.OK, "AUTH2006", "회원탈퇴가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
