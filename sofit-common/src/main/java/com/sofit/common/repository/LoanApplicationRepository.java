package com.sofit.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    // 특정 사용자의 심사 중 상태 목록 조회
    List<LoanApplication> findByUser_UserIdAndStatusIn(Long userId, List<ApplicationStatus> statuses);

    // 특정 사용자의 대출 신청 단건 조회 (본인 소유 검증 포함)
    Optional<LoanApplication> findByApplicationIdAndUser_UserId(Long applicationId, Long userId);

    // 특정 사용자의 심사 완료 상태 목록 조회 (updatedAt 내림차순)
    List<LoanApplication> findByUser_UserIdAndStatusInOrderByUpdatedAtDesc(
            Long userId, List<ApplicationStatus> statuses);
}
