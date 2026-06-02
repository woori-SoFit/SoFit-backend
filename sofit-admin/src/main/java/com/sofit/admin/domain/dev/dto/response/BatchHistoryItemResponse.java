package com.sofit.admin.domain.dev.dto.response;

import java.time.LocalDateTime;

public record BatchHistoryItemResponse(
        Long id,
        String status,
        Integer processedCount,
        Long elapsedSeconds,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
