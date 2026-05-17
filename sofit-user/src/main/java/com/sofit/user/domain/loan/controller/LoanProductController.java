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
public class LoanProductController implements LoanProductControllerDocs {

    private final LoanProductService loanProductService;

    @GetMapping
    public ApiResponse<LoanProductListResponse> getProducts() {
        LoanProductListResponse response = loanProductService.findProducts();
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_LIST_OK, response);
    }

    @GetMapping("/{productId}")
    public ApiResponse<LoanProductDetailResponse> getProduct(@PathVariable Long productId) {
        LoanProductDetailResponse response = loanProductService.findProduct(productId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_DETAIL_OK, response);
    }
}
