package com.sofit.user.domain.report.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT4040", "아직 성장 S등급이 산출되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
