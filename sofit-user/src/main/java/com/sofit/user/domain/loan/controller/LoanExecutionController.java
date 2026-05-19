package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanExecutionController implements LoanExecutionControllerDocs {

    private final LoanExecutionService loanExecutionService;

    // TODO: 세션 인증 구현 후 SecurityContext에서 userId 추출하도록 변경
    private static final Long TEMP_USER_ID = 1L;

    @GetMapping("/{applicationId}/execution")
    @Override
    public ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @PathVariable Long applicationId) {
        LoanExecutionResultResponse response =
                loanExecutionService.findExecutionResult(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_EXECUTION_RESULT_OK, response);
    }
}
