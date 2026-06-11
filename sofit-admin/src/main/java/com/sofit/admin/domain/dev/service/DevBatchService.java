package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.dto.response.BatchStatusResponse;

public interface DevBatchService {

    /**
     * 성장 S등급 배치 실행 이력 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작, null이면 기본값 0)
     * @param size 페이지당 건수 (null이면 기본값 5)
     * @return 페이징된 배치 실행 이력 목록
     */
    BatchHistoryListResponse findBatchHistories(Integer page, Integer size);

    /**
     * 수동 S등급 배치 실행 트리거.
     * 이미 배치가 실행 중이면 409 Conflict 예외를 던진다.
     *
     * @param triggeredBy 배치를 트리거한 관리자의 userId
     */
    void triggerSGradeBatch(Long triggeredBy);

    /**
     * AI 서버의 S등급 배치 실행 상태를 조회한다.
     *
     * @return 배치 상태 정보
     */
    BatchStatusResponse getSGradeBatchStatus();
}
