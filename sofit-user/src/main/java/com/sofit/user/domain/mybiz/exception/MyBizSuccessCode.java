package com.sofit.user.domain.mybiz.exception;

import org.springframework.http.HttpStatus;

import com.sofit.common.apiPayload.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MyBizSuccessCode implements BaseSuccessCode {

    DASHBOARD_OK(HttpStatus.OK, "MYBIZ2001", "성공입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
