package com.sofit.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ProductStatus;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    List<LoanProduct> findByStatus(ProductStatus status);
}
