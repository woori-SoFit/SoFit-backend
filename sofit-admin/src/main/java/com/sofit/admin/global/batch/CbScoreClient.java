package com.sofit.admin.global.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 외부 Mock 서버에서 CB 신용점수를 조회하는 클라이언트.
 * 대출 심사 배치(loanDecisionJob)에서만 사용한다.
 *
 * API: POST /ext/cb/inquiry
 * Request: { "name": "...", "residentNumber": "..." }
 * Response: { "isSuccess": true, "result": { "creditScore": 850 } }
 */
@Slf4j
@Component
public class CbScoreClient {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final RestClient restClient;

    public CbScoreClient(@Value("${external.mock.url}") String externalMockUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(externalMockUrl)
                .build();
    }

    /**
     * 외부 Mock 서버에서 CB 신용점수를 조회한다.
     * 실패 시 최대 3회 재시도 (2초 간격).
     *
     * @param name 사용자 이름
     * @param residentNumber 주민번호 앞 7자리
     * @return CB 점수 (조회 실패 시 null)
     */
    public Integer getCbScore(String name, String residentNumber) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                CbInquiryResponse response = restClient.post()
                        .uri("/ext/cb/inquiry")
                        .body(Map.of("name", name, "residentNumber", residentNumber))
                        .retrieve()
                        .body(CbInquiryResponse.class);

                if (response != null && response.isSuccess() && response.result() != null) {
                    return response.result().creditScore();
                }
                log.warn("[CbScoreClient] name={} CB 점수 조회 실패 (시도 {}/{}) — 응답 실패", mask(name), attempt, MAX_RETRIES);
            } catch (RestClientException e) {
                log.warn("[CbScoreClient] name={} CB 점수 조회 실패 (시도 {}/{}): {}", mask(name), attempt, MAX_RETRIES, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("[CbScoreClient] name={} CB 점수 조회 최종 실패 ({}회 재시도 소진)", mask(name), MAX_RETRIES);
        return null;
    }

    /** 로그용 고객명 마스킹 (PII 평문 출력 금지). 예: 홍길동 → 홍** */
    private static String mask(String name) {
        if (name == null || name.isBlank()) {
            return "(unknown)";
        }
        if (name.length() <= 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    // === 응답 DTO ===

    private record CbInquiryResponse(boolean isSuccess, String code, String message, CbResult result) {}

    private record CbResult(String name, Integer creditScore, String evaluatedAt) {}
}
