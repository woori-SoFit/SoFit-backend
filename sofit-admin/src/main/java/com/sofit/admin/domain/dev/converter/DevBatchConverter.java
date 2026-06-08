package com.sofit.admin.domain.dev.converter;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryItemResponse;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.common.entity.sGrade.BatchExecutionHistory;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.util.List;

/**
 * BatchExecutionHistory Entity → 배치 이력 조회 DTO 변환
 */
public class DevBatchConverter {

    private DevBatchConverter() {
    }

    public static BatchHistoryItemResponse toBatchHistoryItemResponse(BatchExecutionHistory history) {
        Long elapsedSeconds = null;
        if (history.getCompletedAt() != null && history.getStartedAt() != null) {
            elapsedSeconds = Duration.between(history.getStartedAt(), history.getCompletedAt()).getSeconds();
        }

        return new BatchHistoryItemResponse(
                history.getExecutionId(),
                history.getStatus(),
                history.getSuccessCount(),
                elapsedSeconds,
                history.getErrorMessage(),
                history.getStartedAt(),
                history.getCompletedAt()
        );
    }

    public static BatchHistoryListResponse toBatchHistoryListResponse(Page<BatchExecutionHistory> page, int currentPage, int size) {
        List<BatchHistoryItemResponse> contents = page.getContent().stream()
                .map(DevBatchConverter::toBatchHistoryItemResponse)
                .toList();

        return new BatchHistoryListResponse(
                contents,
                page.getTotalElements(),
                page.getTotalPages(),
                currentPage,
                size
        );
    }
}
