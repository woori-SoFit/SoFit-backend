package com.sofit.user.domain.loan.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoanProductListResponse {

    private List<LoanProductItem> loanProducts;

    @Getter
    @Builder
    public static class LoanProductItem {
        private Long productId;
        private String productName;
        private String title;
        private Long maxLimit;
    }
}
