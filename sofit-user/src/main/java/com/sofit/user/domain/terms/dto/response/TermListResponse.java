package com.sofit.user.domain.terms.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TermListResponse(
        List<TermItem> terms
) {

    public record TermItem(
            Long termId,
            String termType,
            String version,
            String title,
            String fileUrl,
            Boolean isRequired,
            LocalDateTime effectiveAt
    ) {
    }
}
