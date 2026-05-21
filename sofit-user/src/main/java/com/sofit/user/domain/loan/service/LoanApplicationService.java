package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;

public interface LoanApplicationService {

    // 대출 신청 생성 (DRAFT)
    LoanApplicationCreateResponse createApplication(Long userId, Long productId, LoanApplicationCreateRequest request);

    // DRAFT 존재 여부 확인
    DraftCheckResponse checkDraft(Long userId, Long productId);

    // 이어가기 데이터 조회
    LoanApplicationResumeResponse getResumeData(Long userId, Long applicationId);
}
