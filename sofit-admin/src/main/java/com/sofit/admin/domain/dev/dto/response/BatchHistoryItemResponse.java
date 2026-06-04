package com.sofit.admin.domain.dev.dto.response;

import com.sofit.admin.domain.dev.entity.enums.BatchStatus;

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
