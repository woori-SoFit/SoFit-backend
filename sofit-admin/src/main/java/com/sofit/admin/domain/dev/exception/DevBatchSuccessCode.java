package com.sofit.admin.domain.dev.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DevBatchSuccessCode implements BaseSuccessCode {

    BATCH_TRIGGERED(HttpStatus.ACCEPTED, "BATCH2021", "배치 실행이 시작되었습니다."),
    BATCH_STATUS_OK(HttpStatus.OK, "BATCH2001", "배치 상태 조회에 성공했습니다."),
    LOAN_DECISION_BATCH_HISTORY_OK(HttpStatus.OK, "BATCH2002", "대출 심사 배치 이력 조회에 성공했습니다."),
    LOAN_DECISION_BATCH_TRIGGERED(HttpStatus.ACCEPTED, "BATCH2022", "대출 심사 배치 실행이 시작되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
