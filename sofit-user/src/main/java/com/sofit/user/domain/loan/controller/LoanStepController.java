package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanStepService;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-applications/{applicationId}")
@RequiredArgsConstructor
public class LoanStepController implements LoanStepControllerDocs {

    private final LoanStepService loanStepService;

    // Step 2: 대출 약관 동의
    @PostMapping("/consents")
    @Override
    public ApiResponse<ConsentCreateResponse> processConsent(
            @PathVariable Long applicationId,
            @Valid @RequestBody ConsentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsentCreateResponse response = loanStepService.processConsent(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_STEP_CONSENT_OK, response);
    }

    // Step 3: 사업자 정보 확인
    @PostMapping("/biz-info")
    @Override
    public ApiResponse<BusinessProfileResponse> processBizInfo(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        BusinessProfileResponse response = loanStepService.processBizInfo(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_STEP_BIZ_INFO_OK, response);
    }

    // Step 4: 마이데이터 약관 동의
    @PostMapping("/mydata")
    @Override
    public ApiResponse<ConsentCreateResponse> processMydata(
            @PathVariable Long applicationId,
            @Valid @RequestBody ConsentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsentCreateResponse response = loanStepService.processMydata(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_STEP_MYDATA_OK, response);
    }

    // Step 5: 마이비즈데이터 연동 완료
    @PostMapping("/mybiz-data")
    @Override
    public ApiResponse<Void> processMybizData(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        loanStepService.processMybizData(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_STEP_MYBIZ_OK, null);
    }
}
