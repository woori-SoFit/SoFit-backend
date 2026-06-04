package com.sofit.admin.domain.dev.dto.response;

import java.util.List;

public record BatchHistoryListResponse(
        List<BatchHistoryItemResponse> contents,
        long totalCount,
        int totalPages,
        int currentPage,
        int size
) {
}
