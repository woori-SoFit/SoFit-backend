package com.sofit.user.domain.user.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessCode implements BaseSuccessCode {

    USER_NOT_AUTHENTICATED(HttpStatus.OK, "USER2000", "로그인되지 않은 사용자입니다."),
    USER_PROFILE_OK(HttpStatus.OK, "USER2001", "회원 정보 조회에 성공했습니다."),
    WITHDRAW_SUCCESS(HttpStatus.OK, "USER2002", "회원탈퇴가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
