package com.sofit.user.domain.report.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {

    GRADE_OK(HttpStatus.OK, "REPORT2000", "성장 S등급 조회에 성공했습니다."),
    GRADE_DETAIL_OK(HttpStatus.OK, "REPORT2001", "성장 S등급 상세 리포트 조회에 성공했습니다."),
    MYBIZ_STATUS_OK(HttpStatus.OK, "REPORT2002", "마이비즈 연동 여부 확인에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
