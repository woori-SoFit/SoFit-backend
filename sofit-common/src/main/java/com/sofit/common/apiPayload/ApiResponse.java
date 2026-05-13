package com.sofit.common.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofit.common.apiPayload.code.BaseErrorCode;
import com.sofit.common.apiPayload.code.BaseSuccessCode;
import com.sofit.common.apiPayload.code.GeneralSuccessCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    // 성공 응답 (result 포함)
    public static <T> ApiResponse<T> onSuccess(T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(GeneralSuccessCode.OK.getCode())
                .message(GeneralSuccessCode.OK.getMessage())
                .result(result)
                .build();
    }

    // 성공 응답 (커스텀 코드)
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode successCode, T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .result(result)
                .build();
    }

    // 성공 응답 (result 없음)
    public static <T> ApiResponse<T> onSuccess() {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(GeneralSuccessCode.OK.getCode())
                .message(GeneralSuccessCode.OK.getMessage())
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

    // 실패 응답 (커스텀 메시지 오버라이드)
    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .isSuccess(false)
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
