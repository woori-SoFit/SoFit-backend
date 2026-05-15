package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;

import java.util.List;

public class LoanProductConverter {

    private LoanProductConverter() {
    }

    public static LoanProductListResponse toListResponse(List<LoanProduct> products) {
        List<LoanProductListResponse.LoanProductItem> items = products.stream()
                .map(product -> LoanProductListResponse.LoanProductItem.builder()
                        .productId(product.getProductId())
                        .productName(product.getProductName())
                        .title(product.getTitle())
                        .maxLimit(product.getMaxLimit())
                        .build())
                .toList();

        return LoanProductListResponse.builder()
                .loanProducts(items)
                .build();
    }

    public static LoanProductDetailResponse toDetailResponse(LoanProduct product) {
        return LoanProductDetailResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .title(product.getTitle())
                .subtitle(product.getSubtitle())
                .minLimit(product.getMinLimit())
                .maxLimit(product.getMaxLimit())
                .maxTerm(product.getMaxTerm())
                .targetDescription(product.getTargetDescription())
                .interestRate(LoanProductDetailResponse.InterestRate.builder()
                        .minRate(product.getMinRate())
                        .maxRate(product.getMaxRate())
                        .build())
                .build();
    }
}
