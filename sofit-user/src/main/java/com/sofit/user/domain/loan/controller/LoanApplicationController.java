package com.sofit.user.domain.loan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanApplicationService;
import com.sofit.user.domain.loan.service.LoanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoanApplicationController implements LoanApplicationControllerDocs {

    private final LoanService loanService;
    private final LoanApplicationService loanApplicationService;

    // === 대출 신청 생성 & 이어가기 API ===

    /**
     * 대출 신청 생성 (1차 필터링 통과 후)
     * POST /api/loan-products/{productId}/applications
     */
    @PostMapping("/loan-products/{productId}/applications")
    public ApiResponse<LoanApplicationCreateResponse> createApplication(
            @PathVariable Long productId,
            @Valid @RequestBody LoanApplicationCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        LoanApplicationCreateResponse response = loanApplicationService.createApplication(userId, productId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_CREATED, response);
    }

    /**
     * DRAFT 존재 여부 확인
     * GET /api/loan-applications/draft?productId={productId}
     */
    @GetMapping("/loan-applications/draft")
    public ApiResponse<DraftCheckResponse> checkDraft(
            @RequestParam Long productId,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        DraftCheckResponse response = loanApplicationService.checkDraft(userId, productId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_DRAFT_CHECK_OK, response);
    }

    /**
     * 이어가기 데이터 조회
     * GET /api/loan-applications/{applicationId}/resume
     */
    @GetMapping("/loan-applications/{applicationId}/resume")
    public ApiResponse<LoanApplicationResumeResponse> getResumeData(
            @PathVariable Long applicationId,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        LoanApplicationResumeResponse response = loanApplicationService.getResumeData(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_RESUME_OK, response);
    }

    /**
     * 최종 제출 (심사 요청)
     * POST /api/loan-applications/{applicationId}/submit
     */
    @PostMapping("/loan-applications/{applicationId}/submit")
    public ApiResponse<LoanApplicationSubmitResponse> submitApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody LoanApplicationSubmitRequest request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        LoanApplicationSubmitResponse response = loanApplicationService.submitApplication(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_SUBMIT_OK, response);
    }

    // === 기존 심사 현황 조회 API ===

    // TODO: 세션 인증 구현 후 SecurityContext에서 userId 추출하도록 변경
    private static final Long TEMP_USER_ID = 1L;

    /**
     * 심사 중인 대출 목록 조회
     * GET /api/loan-applications
     */
    @GetMapping("/loan-applications")
    public ApiResponse<LoanApplicationListResponse> getUnderReviewLoans() {
        LoanApplicationListResponse response = loanService.findUnderReviewLoans(TEMP_USER_ID);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_LIST_OK, response);
    }

    /**
     * 심사 중인 대출 상세 조회
     * GET /api/loan-applications/{applicationId}
     */
    @GetMapping("/loan-applications/{applicationId}")
    public ApiResponse<LoanApplicationDetailResponse> getLoanDetail(
            @PathVariable Long applicationId) {
        LoanApplicationDetailResponse response = loanService.findLoanDetail(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_DETAIL_OK, response);
    }

    /**
     * 심사 완료 대출 목록 조회
     * GET /api/loan-applications/completed
     */
    @GetMapping("/loan-applications/completed")
    public ApiResponse<CompletedLoanListResponse> getCompletedLoans() {
        CompletedLoanListResponse response = loanService.findCompletedLoans(TEMP_USER_ID);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_LIST_OK, response);
    }

    /**
     * 심사 완료 대출 상세 조회
     * GET /api/loan-applications/completed/{applicationId}
     */
    @GetMapping("/loan-applications/completed/{applicationId}")
    public ApiResponse<CompletedLoanDetailResponse> getCompletedLoanDetail(
            @PathVariable Long applicationId) {
        CompletedLoanDetailResponse response = loanService.findCompletedLoanDetail(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_DETAIL_OK, response);
    }

    // === Private Helper ===

    /**
     * 세션에서 userId를 추출한다.
     * 세션이 없거나 userId가 없으면 UNAUTHORIZED 예외 발생
     */
    private Long extractUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new BaseException(GeneralErrorCode.UNAUTHORIZED);
        }
        Object userIdAttr = session.getAttribute("userId");
        return (userIdAttr instanceof Long) ? (Long) userIdAttr : Long.valueOf(userIdAttr.toString());
    }
}
