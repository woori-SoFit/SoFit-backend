package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductOptionsResponse;

public interface LoanProductService {

    LoanProductListResponse findProducts();

    LoanProductDetailResponse findProduct(Long productId);

    LoanProductOptionsResponse findProductOptions(Long productId);
}
