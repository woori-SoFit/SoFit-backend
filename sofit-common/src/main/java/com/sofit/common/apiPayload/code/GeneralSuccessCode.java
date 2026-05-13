package com.sofit.common.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK, "COMMON2000", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON2001", "생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
