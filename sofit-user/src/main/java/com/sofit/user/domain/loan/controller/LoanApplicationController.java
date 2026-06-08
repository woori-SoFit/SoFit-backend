package com.sofit.user.domain.loan.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanApplicationService;
import com.sofit.user.domain.loan.service.LoanService;
import com.sofit.user.global.util.SecurityUtil;

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
            @Valid @RequestBody LoanApplicationCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanApplicationCreateResponse response = loanApplicationService.createApplication(userId, productId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_CREATED, response);
    }

    /**
     * DRAFT 존재 여부 확인
     * GET /api/loan-applications/draft?productId={productId}
     */
    @GetMapping("/loan-applications/draft")
    public ApiResponse<DraftCheckResponse> checkDraft(
            @RequestParam Long productId) {
        Long userId = SecurityUtil.getCurrentUserId();
        DraftCheckResponse response = loanApplicationService.checkDraft(userId, productId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_DRAFT_CHECK_OK, response);
    }

    /**
     * 진행 중인 DRAFT 목록 조회
     * GET /api/loan-applications/drafts
     */
    @GetMapping("/loan-applications/drafts")
    public ApiResponse<DraftListResponse> getDrafts() {
        Long userId = SecurityUtil.getCurrentUserId();
        DraftListResponse response = loanApplicationService.findDrafts(userId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_DRAFT_LIST_OK, response);
    }

    /**
     * 이어가기 데이터 조회
     * GET /api/loan-applications/{applicationId}/resume
     */
    @GetMapping("/loan-applications/{applicationId}/resume")
    public ApiResponse<LoanApplicationResumeResponse> getResumeData(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanApplicationResumeResponse response = loanApplicationService.getResumeData(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_RESUME_OK, response);
    }

    /**
     * DRAFT 신청서 취소 (소프트 삭제)
     * DELETE /api/loan-applications/{applicationId}
     */
    @DeleteMapping("/loan-applications/{applicationId}")
    public ApiResponse<Void> cancelDraftApplication(@PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        loanApplicationService.cancelDraftApplication(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_DRAFT_CANCELLED, null);
    }

    /**
     * 최종 제출 (심사 요청)
     * POST /api/loan-applications/{applicationId}/submit
     */
    @PostMapping("/loan-applications/{applicationId}/submit")
    public ApiResponse<LoanApplicationSubmitResponse> submitApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody LoanApplicationSubmitRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanApplicationSubmitResponse response = loanApplicationService.submitApplication(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_SUBMIT_OK, response);
    }

    // === 기존 심사 현황 조회 API ===

    /**
     * 심사 중인 대출 목록 조회
     * GET /api/loan-applications
     */
    @GetMapping("/loan-applications")
    public ApiResponse<LoanApplicationListResponse> getUnderReviewLoans() {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanApplicationListResponse response = loanService.findUnderReviewLoans(userId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_LIST_OK, response);
    }

    /**
     * 심사 중인 대출 상세 조회
     * GET /api/loan-applications/{applicationId}
     */
    @GetMapping("/loan-applications/{applicationId}")
    public ApiResponse<LoanApplicationDetailResponse> getLoanDetail(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanApplicationDetailResponse response = loanService.findLoanDetail(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_DETAIL_OK, response);
    }

    /**
     * 심사 완료 대출 목록 조회
     * GET /api/loan-applications/completed
     */
    @GetMapping("/loan-applications/completed")
    public ApiResponse<CompletedLoanListResponse> getCompletedLoans() {
        Long userId = SecurityUtil.getCurrentUserId();
        CompletedLoanListResponse response = loanService.findCompletedLoans(userId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_LIST_OK, response);
    }

    /**
     * 심사 완료 대출 상세 조회
     * GET /api/loan-applications/completed/{applicationId}
     */
    @GetMapping("/loan-applications/completed/{applicationId}")
    public ApiResponse<CompletedLoanDetailResponse> getCompletedLoanDetail(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        CompletedLoanDetailResponse response = loanService.findCompletedLoanDetail(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_DETAIL_OK, response);
    }
}
