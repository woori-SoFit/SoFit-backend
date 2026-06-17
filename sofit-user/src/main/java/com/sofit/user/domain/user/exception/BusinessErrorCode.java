package com.sofit.user.domain.user.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessErrorCode implements BaseErrorCode {

    BUSINESS_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "BUSINESS4040", "사업자 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
