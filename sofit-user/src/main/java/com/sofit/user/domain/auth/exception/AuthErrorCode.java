package com.sofit.user.domain.auth.exception;

import com.sofit.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 400 Bad Request
    PIN_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH4001", "PIN 번호가 올바르지 않습니다."),
    INVALID_INPUT_FORMAT(HttpStatus.BAD_REQUEST, "AUTH4002", "입력값 형식이 올바르지 않습니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "AUTH4008", "아이디는 영문/숫자 4~20자여야 합니다."),
    BUSINESS_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "AUTH4003", "이미 가입된 사업자등록번호입니다."),
    STEP_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "AUTH4004", "이전 단계가 완료되지 않았습니다."),
    STEP_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "AUTH4007", "이미 완료된 단계입니다."),
    REGISTRATION_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH4005", "인증 정보가 만료되었습니다."),
    CERT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "AUTH4006", "금융인증서 검증에 실패했습니다."),

    // 401 Unauthorized
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4011", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // 429 Too Many Requests
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "AUTH4291", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // 403 Forbidden
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "AUTH4031", "탈퇴한 계정입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4040", "존재하지 않는 사용자입니다."),
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4041", "일치하는 사업자등록번호를 찾을 수 없습니다."),
    CERT_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4042", "등록된 금융인증서를 찾을 수 없습니다."),

    // 409 Conflict
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "AUTH4091", "이미 사용 중인 아이디입니다."),

    // 502 Bad Gateway
    EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH5021", "외부 인증 서버와 통신 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
