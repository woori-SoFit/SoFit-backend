package com.sofit.user.domain.auth.service;

import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;

/**
 * 금융인증서 본인인증 + PIN 검증을 담당하는 서비스 인터페이스.
 * 세션, HTTP, Spring Security 등 웹 레이어 의존성 없음.
 * 회원가입/대출신청/마이비즈 등 어느 플로우에서든 재사용 가능.
 */
public interface FinancialCertService {

    /**
     * 금융인증서 본인인증 + PIN 검증을 수행한다.
     * 성공 시 정상 반환, 실패 시 예외를 던진다.
     */
    void verify(FinancialCertVerifyRequest request);
}
