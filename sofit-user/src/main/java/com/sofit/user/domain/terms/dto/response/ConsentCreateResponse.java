package com.sofit.user.domain.terms.dto.response;

import com.sofit.common.entity.term.enums.TermType;

import java.time.LocalDateTime;
import java.util.List;

public record ConsentCreateResponse(
        TermType termType,
        Long applicationId,
        Long userId,
        List<ConsentItemResponse> consents
) {
    public record ConsentItemResponse(
            Long termId,
            Boolean isConsented,
            LocalDateTime consentedAt
    ) {}
}
