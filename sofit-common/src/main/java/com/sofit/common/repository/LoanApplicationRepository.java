package com.sofit.common.repository;

import com.sofit.common.entity.loan.ApplicationStatus;
import com.sofit.common.entity.loan.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByUserIdAndProductProductId(Long userId, Long productId);

    List<LoanApplication> findByUserId(Long userId);

    Optional<LoanApplication> findByApplicationIdAndUserId(Long applicationId, Long userId);

    List<LoanApplication> findByStatus(ApplicationStatus status);
}
