package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductOptionsResponse;

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
                        .minRate(product.getMinRate())
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
                .targetSummary(product.getTargetSummary())
                .filterConditions(LoanProductDetailResponse.FilterConditions.builder()
                        .annualIncomeLimit(product.getAnnualIncomeLimit())
                        .creditScoreLimit(product.getCreditScoreLimit())
                        .incomeTypeCodeLimit(product.getIncomeTypeCodeLimit())
                        .existingLoanAmtLimit(product.getExistingLoanAmtLimit())
                        .build())
                .interestRate(LoanProductDetailResponse.InterestRate.builder()
                        .minRate(product.getMinRate())
                        .maxRate(product.getMaxRate())
                        .build())
                .productDescription(LoanProductDetailResponse.ProductDescription.builder()
                        .targetDetail(product.getTargetDetail())
                        .limitDescription(product.getLimitDescription())
                        .termDescription(product.getTermDescription())
                        .rateDescription(product.getRateDescription())
                        .preferentialRateDescription(product.getPreferentialRateDescription())
                        .repaymentDescription(product.getRepaymentDescription())
                        .collateralDescription(product.getCollateralDescription())
                        .feeDescription(product.getFeeDescription())
                        .build())
                .build();
    }

    public static LoanProductOptionsResponse toOptionsResponse(LoanProduct product, List<LoanProductOption> options) {
        List<LoanProductOptionsResponse.LoanOptionItem> items = options.stream()
                .map(option -> LoanProductOptionsResponse.LoanOptionItem.builder()
                        .purpose(option.getPurpose())
                        .repaymentMethod(option.getRepaymentMethod())
                        .maxTermMonths(option.getMaxTermMonths())
                        .build())
                .toList();

        return LoanProductOptionsResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .minLimit(product.getMinLimit())
                .maxLimit(product.getMaxLimit())
                .loanOptions(items)
                .build();
    }
}
