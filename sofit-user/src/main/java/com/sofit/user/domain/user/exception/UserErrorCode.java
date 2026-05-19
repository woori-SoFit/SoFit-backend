package com.sofit.user.domain.user.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    // 403 Forbidden
    INACTIVE_USER(HttpStatus.FORBIDDEN, "AUTH4031", "탈퇴한 계정입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
