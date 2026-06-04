package com.sofit.admin.domain.dev.dto.response;

import com.sofit.common.entity.sGrade.enums.BatchStatus;

import java.time.LocalDateTime;

public record BatchHistoryItemResponse(
        Long id,
        BatchStatus status,
        Integer processedCount,
        Long elapsedSeconds,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
