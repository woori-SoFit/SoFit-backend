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

    // 동일 상품 중복 신청 체크 (CANCELLED 제외한 모든 상태에 신청이 존재하는지)
    boolean existsByUser_UserIdAndProduct_ProductIdAndStatusNot(
            Long userId, Long productId, ApplicationStatus status);

    // 특정 상품에 대한 DRAFT 상태 신청 조회
    Optional<LoanApplication> findByUser_UserIdAndProduct_ProductIdAndStatus(
            Long userId, Long productId, ApplicationStatus status);
}
