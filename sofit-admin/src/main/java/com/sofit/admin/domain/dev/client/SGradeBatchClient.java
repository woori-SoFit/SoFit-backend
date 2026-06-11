package com.sofit.admin.domain.dev.client;

import com.sofit.admin.domain.dev.dto.response.BatchStatusResponse;
import com.sofit.admin.domain.dev.exception.DevBatchErrorCode;
import com.sofit.common.apiPayload.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 서버의 S등급 배치 관련 API를 호출하는 클라이언트.
 * - POST /api/s-grade/batch?triggered_by={userId} : 배치 실행 트리거
 * - GET /api/s-grade/batch/status : 배치 상태 조회
 */
@Slf4j
@Component
public class SGradeBatchClient {

    private final RestClient restClient;

    public SGradeBatchClient(@Value("${sofit.ai.server.url}") String aiServerUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 3초
        factory.setReadTimeout(10000);     // 10초

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * AI 서버에 S등급 배치 실행을 트리거한다.
     * AI 서버는 202 Accepted를 즉시 반환하고 비동기로 배치를 수행한다.
     *
     * @param triggeredBy 배치를 트리거한 관리자의 userId
     */
    public void triggerBatch(Long triggeredBy) {
        try {
            restClient.post()
                    .uri("/api/s-grade/batch?triggered_by={triggeredBy}", triggeredBy)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SGradeBatchClient] 배치 트리거 성공 (triggered_by={})", triggeredBy);
        } catch (RestClientException e) {
            log.error("[SGradeBatchClient] AI 서버 배치 트리거 실패: {}", e.getMessage());
            throw new BaseException(DevBatchErrorCode.AI_SERVER_UNAVAILABLE);
        }
    }

    /**
     * AI 서버의 배치 실행 상태를 조회한다.
     *
     * @return 배치 상태 정보 (status, total, completed, failed 등)
     */
    public BatchStatusResponse getBatchStatus() {
        try {
            BatchStatusResponse response = restClient.get()
                    .uri("/api/s-grade/batch/status")
                    .retrieve()
                    .body(BatchStatusResponse.class);

            log.debug("[SGradeBatchClient] 배치 상태 조회 성공: status={}", response != null ? response.status() : "null");
            return response;
        } catch (RestClientException e) {
            log.error("[SGradeBatchClient] AI 서버 상태 조회 실패: {}", e.getMessage());
            throw new BaseException(DevBatchErrorCode.AI_SERVER_UNAVAILABLE);
        }
    }
}
