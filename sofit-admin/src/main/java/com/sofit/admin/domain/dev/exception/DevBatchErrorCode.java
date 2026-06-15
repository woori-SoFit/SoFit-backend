package com.sofit.admin.domain.dev.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DevBatchErrorCode implements BaseErrorCode {

    BATCH_ALREADY_RUNNING(HttpStatus.CONFLICT, "BATCH4091", "이미 배치가 실행 중입니다."),
    LOAN_DECISION_BATCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BATCH5001", "대출 심사 배치 실행에 실패했습니다."),
    AI_SERVER_REJECTED(HttpStatus.BAD_GATEWAY, "BATCH5021", "AI 서버가 요청을 처리할 수 없습니다. 관리자에게 문의하세요."),
    AI_SERVER_INTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "BATCH5022", "AI 서버에 내부 오류가 발생했습니다."),
    AI_SERVER_EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "BATCH5023", "AI 서버 응답이 비어있습니다."),
    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BATCH5031", "AI 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
