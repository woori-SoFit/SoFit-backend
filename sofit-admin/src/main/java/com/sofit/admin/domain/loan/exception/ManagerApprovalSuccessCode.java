package com.sofit.admin.domain.loan.exception;

import com.sofit.common.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ManagerApprovalSuccessCode implements BaseSuccessCode {

    MANAGER_APPROVAL_LIST_OK(HttpStatus.OK, "COMMON2000", "성공입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
