package com.sofit.admin.domain.dev.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DevBatchErrorCode implements BaseErrorCode {

    BATCH_ALREADY_RUNNING(HttpStatus.CONFLICT, "BATCH4091", "이미 배치가 실행 중입니다."),
    AI_SERVER_BAD_REQUEST(HttpStatus.BAD_GATEWAY, "BATCH5021", "AI 서버가 요청을 거부했습니다."),
    AI_SERVER_INTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "BATCH5022", "AI 서버 내부 오류가 발생했습니다."),
    AI_SERVER_EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "BATCH5023", "AI 서버 응답이 비어있습니다."),
    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BATCH5031", "AI 서버 연결에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
