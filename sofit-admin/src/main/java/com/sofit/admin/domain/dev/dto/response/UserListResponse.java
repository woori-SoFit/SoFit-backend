package com.sofit.admin.domain.dev.dto.response;

import java.util.List;

public record UserListResponse(
        List<UserItemResponse> contents,
        long totalCount,
        int totalPages,
        int currentPage,
        int size
) {
}
