package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;

public interface LoanApplicationService {

    // 대출 신청 생성 (DRAFT)
    LoanApplicationCreateResponse createApplication(Long userId, Long productId, LoanApplicationCreateRequest request);

    // DRAFT 존재 여부 확인
    DraftCheckResponse checkDraft(Long userId, Long productId);

    // 사용자의 전체 DRAFT 목록 조회
    DraftListResponse findDrafts(Long userId);

    // 이어가기 데이터 조회
    LoanApplicationResumeResponse getResumeData(Long userId, Long applicationId);

    // 최종 제출 (심사 요청) — DRAFT → SUBMITTED
    LoanApplicationSubmitResponse submitApplication(Long userId, Long applicationId, LoanApplicationSubmitRequest request);

    // DRAFT 신청서 취소 (소프트 삭제)
    void cancelDraftApplication(Long userId, Long applicationId);
}
