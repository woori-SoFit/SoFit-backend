package com.sofit.common.repository.loan;

import com.sofit.common.entity.loan.LoanRatePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoanRatePolicyRepository extends JpaRepository<LoanRatePolicy, Long> {

    /**
     * product_id와 scbGrade 기준으로 해당 구간의 금리 정책 조회
     * 조건: min_score <= scbGrade < max_score
     */
    @Query("SELECT lrp FROM LoanRatePolicy lrp " +
           "WHERE lrp.product.productId = :productId " +
           "AND lrp.minScore <= :scbGrade " +
           "AND lrp.maxScore > :scbGrade")
    Optional<LoanRatePolicy> findByProductIdAndScbGrade(
            @Param("productId") Long productId,
            @Param("scbGrade") Integer scbGrade);
}
