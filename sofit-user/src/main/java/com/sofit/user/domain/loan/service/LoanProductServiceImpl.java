package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.repository.LoanProductRepository;
import com.sofit.user.domain.loan.converter.LoanProductConverter;
import com.sofit.user.domain.loan.dto.response.LoanProductDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanProductListResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanProductServiceImpl implements LoanProductService {

    private final LoanProductRepository loanProductRepository;

    @Override
    public LoanProductListResponse findProducts() {
        List<LoanProduct> products = loanProductRepository.findByStatus(ProductStatus.ACTIVE);
        return LoanProductConverter.toListResponse(products);
    }

        @Override
    public LoanProductDetailResponse findProduct(Long productId) {
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.PRODUCT_NOT_FOUND));
        return LoanProductConverter.toDetailResponse(product);
    }
}
