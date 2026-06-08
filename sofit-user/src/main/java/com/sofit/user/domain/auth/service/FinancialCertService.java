package com.sofit.user.domain.auth.service;

import com.sofit.user.domain.auth.dto.request.FinancialCertLookupRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.FinancialCertLookupResponse;

/**
 * 금융인증서 조회 및 본인인증 + PIN 검증을 담당하는 서비스 인터페이스.
 * 세션, HTTP, Spring Security 등 웹 레이어 의존성 없음.
 * 회원가입/대출신청/마이비즈 등 어느 플로우에서든 재사용 가능.
 */
public interface FinancialCertService {

    /**
     * 금융인증서를 조회한다.
     * PIN 없이 본인 정보만으로 인증서 존재 여부를 확인하고 인증서 정보를 반환한다.
     */
    FinancialCertLookupResponse lookup(FinancialCertLookupRequest request);

    /**
     * 금융인증서 본인인증 + PIN 검증을 수행한다.
     * 성공 시 정상 반환, 실패 시 예외를 던진다.
     */
    void verify(FinancialCertVerifyRequest request);
}
