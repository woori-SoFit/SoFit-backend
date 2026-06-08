package com.sofit.common.repository.loan;

import com.sofit.common.entity.loan.LoanProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanProductOptionRepository extends JpaRepository<LoanProductOption, Long> {

    List<LoanProductOption> findByProduct_ProductId(Long productId);
}
