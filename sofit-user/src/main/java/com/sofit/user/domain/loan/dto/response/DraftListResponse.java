package com.sofit.user.domain.loan.dto.response;

import java.util.List;

public record DraftListResponse(
        List<DraftItemResponse> drafts
) {
}
