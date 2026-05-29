package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoanDecisionErrorCode implements BaseErrorCode {

    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4041", "대출 신청 건을 찾을 수 없습니다."),
    ALREADY_DECIDED(HttpStatus.CONFLICT, "LOAN4091", "이미 승인/거절 처리된 신청 건입니다."),
    INVALID_COMMENT(HttpStatus.BAD_REQUEST, "LOAN4002", "comment는 필수이며 공백일 수 없습니다."),
    NOT_DECIDABLE_STATUS(HttpStatus.BAD_REQUEST, "LOAN4003", "현재 상태에서는 승인/거절 처리할 수 없습니다."),
    NO_DECISION_AUTHORITY(HttpStatus.FORBIDDEN, "LOAN4031", "해당 신청 건에 대한 심사 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
