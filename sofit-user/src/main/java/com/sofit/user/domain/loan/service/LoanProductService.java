package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;

public interface LoanProductService {

    LoanProductListResponse findProducts();

    LoanProductDetailResponse findProduct(Long productId);
}
