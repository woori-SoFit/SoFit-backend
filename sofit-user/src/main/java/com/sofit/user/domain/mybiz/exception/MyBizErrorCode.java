package com.sofit.user.domain.mybiz.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MyBizErrorCode implements BaseErrorCode {

    MY_BIZ_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "MYBIZ4041", "My Biz Data가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
