package com.sofit.admin.domain.dev.dto.response;

public record UserStatisticsResponse(
        long totalCount,
        long activeCount,
        long bankerCount,
        long userCount,
        long inactiveCount
) {
}
