package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.client.SGradeBatchClient;
import com.sofit.admin.domain.dev.converter.DevBatchConverter;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.dto.response.BatchStatusResponse;
import com.sofit.admin.domain.dev.exception.DevBatchErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.sGrade.BatchExecutionHistory;
import com.sofit.common.repository.sGrade.BatchExecutionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevBatchServiceImpl implements DevBatchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;
    private static final String BATCH_STATUS_RUNNING = "RUNNING";

    /**
     * 동시 요청 방어용 락.
     * getBatchStatus()와 triggerBatch() 사이 race condition을 방지한다.
     * AI 서버 쪽에 중복 실행 방어가 없으므로 Admin Backend에서 1차 방어.
     */
    private final AtomicBoolean triggerInProgress = new AtomicBoolean(false);

    private final BatchExecutionHistoryRepository batchExecutionHistoryRepository;
    private final SGradeBatchClient sGradeBatchClient;

    @Override
    public BatchHistoryListResponse findBatchHistories(Integer page, Integer size) {
        int actualPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int actualSize = (size != null && size >= 1) ? size : DEFAULT_SIZE;

        Pageable pageable = PageRequest.of(actualPage, actualSize, Sort.by(Sort.Direction.DESC, "executionId"));

        Page<BatchExecutionHistory> historyPage = batchExecutionHistoryRepository.findAll(pageable);

        return DevBatchConverter.toBatchHistoryListResponse(historyPage, actualPage, actualSize);
    }

    @Override
    public void triggerSGradeBatch(Long triggeredBy) {
        // 동시 요청 방어: CAS로 한 번에 하나의 트리거 요청만 통과
        if (!triggerInProgress.compareAndSet(false, true)) {
            log.warn("[DevBatch] 동시 트리거 요청 차단 (triggered_by={})", triggeredBy);
            throw new BaseException(DevBatchErrorCode.BATCH_ALREADY_RUNNING);
        }

        try {
            // AI 서버에 현재 배치 실행 중 여부 확인
            BatchStatusResponse currentStatus = sGradeBatchClient.getBatchStatus();
            if (BATCH_STATUS_RUNNING.equals(currentStatus.status())) {
                log.warn("[DevBatch] 배치 실행 중 상태에서 수동 트리거 시도 (triggered_by={})", triggeredBy);
                throw new BaseException(DevBatchErrorCode.BATCH_ALREADY_RUNNING);
            }

            // AI 서버에 배치 트리거
            sGradeBatchClient.triggerBatch(triggeredBy);

            log.info("[DevBatch] 수동 S등급 배치 트리거 완료 (triggered_by={})", triggeredBy);
        } finally {
            triggerInProgress.set(false);
        }
    }

    @Override
    public BatchStatusResponse getSGradeBatchStatus() {
        return sGradeBatchClient.getBatchStatus();
    }
}
