package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;

/**
 * 대출 신청 단계별 래퍼 서비스.
 * 공통 검증(본인 소유, DRAFT 상태, 단계 순서) + 기존 서비스 호출 + lastCompletedStep 업데이트를 담당한다.
 */
public interface LoanStepService {

    // Step 2: 대출 약관 동의
    ConsentCreateResponse processConsent(Long userId, Long applicationId, ConsentCreateRequest request);

    // Step 3: 사업자 정보 확인
    BusinessProfileResponse processBizInfo(Long userId, Long applicationId);

    // Step 4: 마이데이터 약관 동의
    ConsentCreateResponse processMydata(Long userId, Long applicationId, ConsentCreateRequest request);

    // Step 5: 마이비즈데이터 연동 완료
    void processMybizData(Long userId, Long applicationId);
}
