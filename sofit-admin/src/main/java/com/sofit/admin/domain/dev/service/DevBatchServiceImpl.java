package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.converter.DevBatchConverter;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.common.entity.sGrade.BatchExecutionHistory;
import com.sofit.common.repository.sGrade.BatchExecutionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevBatchServiceImpl implements DevBatchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;

    private final BatchExecutionHistoryRepository batchExecutionHistoryRepository;

    @Override
    public BatchHistoryListResponse findBatchHistories(Integer page, Integer size) {
        int actualPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int actualSize = (size != null && size >= 1) ? size : DEFAULT_SIZE;

        Pageable pageable = PageRequest.of(actualPage, actualSize, Sort.by(Sort.Direction.DESC, "executionId"));

        Page<BatchExecutionHistory> historyPage = batchExecutionHistoryRepository.findAll(pageable);

        return DevBatchConverter.toBatchHistoryListResponse(historyPage, actualPage, actualSize);
    }
}
