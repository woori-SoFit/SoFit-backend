package com.sofit.user.global.exception;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(1)
public class UserExceptionHandler {

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[MissingParam] {}", e.getMessage());
        return ResponseEntity
                .status(GeneralErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.BAD_REQUEST));
    }
}
