package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-products")
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    public ApiResponse<LoanProductListResponse> getProducts() {
        LoanProductListResponse result = loanProductService.findProducts();
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_LIST_OK, result);
    }

    @GetMapping("/{productId}")
    public ApiResponse<LoanProductDetailResponse> getProduct(@PathVariable Long productId) {
        LoanProductDetailResponse result = loanProductService.findProduct(productId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_DETAIL_OK, result);
    }
}
