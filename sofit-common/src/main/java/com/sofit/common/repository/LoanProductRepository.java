package com.sofit.common.repository;

import com.sofit.common.entity.loan.LoanProduct;

import com.sofit.common.entity.loan.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    List<LoanProduct> findByStatus(ProductStatus status);
}
