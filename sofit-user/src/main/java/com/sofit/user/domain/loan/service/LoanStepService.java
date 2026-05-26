package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 대출 신청 단계별 래퍼 서비스.
 * 공통 검증(본인 소유, DRAFT 상태, 단계 순서) + 기존 서비스 호출 + lastCompletedStep 업데이트를 담당한다.
 */
public interface LoanStepService {

    // Step 2: 대출 약관 동의
    ConsentCreateResponse processConsent(Long userId, Long applicationId, ConsentCreateRequest request);

    // Step 3: 본인인증 (금융인증서 PIN)
    FinancialCertVerifyResponse processAuth(Long userId, Long applicationId, FinancialCertVerifyRequest request, HttpSession session);

    // Step 4: 사업자 정보 확인
    BusinessProfileResponse processBizInfo(Long userId, Long applicationId);

    // Step 5: 마이데이터 약관 동의
    ConsentCreateResponse processMydata(Long userId, Long applicationId, ConsentCreateRequest request);
}
