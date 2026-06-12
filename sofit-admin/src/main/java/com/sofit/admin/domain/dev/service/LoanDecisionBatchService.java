package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;

public interface LoanDecisionBatchService {

    /**
     * 대출 심사 배치(loanDecisionJob) 실행 이력 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작, null이면 기본값 0)
     * @param size 페이지당 건수 (null이면 기본값 5)
     * @return 페이징된 배치 실행 이력 목록
     */
    BatchHistoryListResponse findBatchHistories(Integer page, Integer size);
}
