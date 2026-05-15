package com.sofit.common.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sofit.common.apiPayload.code.BaseErrorCode;
import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    // 성공 응답
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode successCode, T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .result(result)
                .build();
    }


    // 실패 응답
    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .isSuccess(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}
