package com.sofit.user.domain.user.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessSuccessCode implements BaseSuccessCode {

    BUSINESS_PROFILE_OK(HttpStatus.OK, "BUSINESS2001", "사업자 정보 조회에 성공했습니다."),
    MYBIZ_CONNECT_OK(HttpStatus.OK, "BUSINESS2002", "마이 비즈 데이터 연동이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
