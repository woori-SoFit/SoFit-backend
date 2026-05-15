package com.sofit.user.domain.loan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    // 특정 사용자의 심사 중 상태 목록 조회
    List<LoanApplication> findByUser_IdAndStatusIn(Long userId, List<ApplicationStatus> statuses);

    // 특정 사용자의 대출 신청 단건 조회 (본인 소유 검증 포함)
    Optional<LoanApplication> findByApplicationIdAndUser_Id(Long applicationId, Long userId);
}
