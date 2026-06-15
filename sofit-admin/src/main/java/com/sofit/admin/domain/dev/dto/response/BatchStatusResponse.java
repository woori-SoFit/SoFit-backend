package com.sofit.admin.domain.dev.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record BatchStatusResponse(
        String status,
        Integer total,
        Integer completed,
        Integer failed,
        Integer calculating,
        @JsonProperty("started_at")
        LocalDateTime startedAt,
        @JsonProperty("completed_at")
        LocalDateTime completedAt
) {
}
