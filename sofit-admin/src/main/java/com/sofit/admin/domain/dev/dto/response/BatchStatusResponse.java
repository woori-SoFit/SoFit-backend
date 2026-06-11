package com.sofit.admin.domain.dev.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchStatusResponse(
        String status,
        Integer total,
        Integer completed,
        Integer failed,
        Integer calculating,
        @JsonProperty("started_at") String startedAt,
        @JsonProperty("completed_at") String completedAt
) {
}
