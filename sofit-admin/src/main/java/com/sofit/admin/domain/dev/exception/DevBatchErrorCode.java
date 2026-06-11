package com.sofit.admin.domain.dev.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DevBatchErrorCode implements BaseErrorCode {

    BATCH_ALREADY_RUNNING(HttpStatus.CONFLICT, "BATCH4091", "이미 배치가 실행 중입니다."),
    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BATCH5031", "AI 서버 연결에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
