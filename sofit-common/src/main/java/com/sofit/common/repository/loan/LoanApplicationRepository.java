package com.sofit.common.repository.loan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.projection.StatusCountProjection;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    // 대시보드 조회: status 필터만 적용 (JOIN FETCH User, LoanProduct)
    @Query(value = "SELECT la FROM LoanApplication la " +
            "JOIN FETCH la.user u " +
            "JOIN FETCH la.product p " +
            "WHERE la.status IN :statuses " +
            "ORDER BY la.appliedAt DESC",
            countQuery = "SELECT COUNT(la) FROM LoanApplication la WHERE la.status IN :statuses")
    Page<LoanApplication> findDashboardApplications(
            @Param("statuses") List<ApplicationStatus> statuses, Pageable pageable);

    // 대시보드 조회: status + assignedBankerId 필터 적용
    @Query(value = "SELECT la FROM LoanApplication la " +
            "JOIN FETCH la.user u " +
            "JOIN FETCH la.product p " +
            "WHERE la.status IN :statuses AND la.assignedBankerId = :assignedBankerId " +
            "ORDER BY la.appliedAt DESC",
            countQuery = "SELECT COUNT(la) FROM LoanApplication la " +
                    "WHERE la.status IN :statuses AND la.assignedBankerId = :assignedBankerId")
    Page<LoanApplication> findDashboardApplicationsByBankerId(
            @Param("statuses") List<ApplicationStatus> statuses,
            @Param("assignedBankerId") Long assignedBankerId,
            Pageable pageable);

    // 특정 사용자의 심사 중 상태 목록 조회
    List<LoanApplication> findByUser_UserIdAndStatusIn(Long userId, List<ApplicationStatus> statuses);

    // 특정 사용자의 대출 신청 단건 조회 (본인 소유 검증 포함)
    Optional<LoanApplication> findByApplicationIdAndUser_UserId(Long applicationId, Long userId);

    // 심사 완료 상세 조회 전용: product fetch join (N+1 방지)
    @Query("SELECT la FROM LoanApplication la " +
           "JOIN FETCH la.product " +
           "WHERE la.applicationId = :applicationId " +
           "AND la.user.userId = :userId")
    Optional<LoanApplication> findCompletedDetailByApplicationIdAndUserId(
            @Param("applicationId") Long applicationId,
            @Param("userId") Long userId);

    // 특정 사용자의 심사 완료 상태 목록 조회 (updatedAt 내림차순)
    List<LoanApplication> findByUser_UserIdAndStatusInOrderByUpdatedAtDesc(
            Long userId, List<ApplicationStatus> statuses);

    // 심사 완료 목록 조회 전용: product fetch join (N+1 방지)
    @Query("SELECT la FROM LoanApplication la " +
           "JOIN FETCH la.product " +
           "WHERE la.user.userId = :userId " +
           "AND la.status IN :statuses " +
           "ORDER BY la.updatedAt DESC")
    List<LoanApplication> findCompletedByUserIdWithProduct(
            @Param("userId") Long userId,
            @Param("statuses") List<ApplicationStatus> statuses);

    // 동일 상품 중복 신청 체크 (특정 상태 제외한 진행 중 신청이 존재하는지)
    boolean existsByUser_UserIdAndProduct_ProductIdAndStatusNotIn(
            Long userId, Long productId, List<ApplicationStatus> statuses);

    // 특정 상품에 대한 DRAFT 상태 신청 조회
    Optional<LoanApplication> findByUser_UserIdAndProduct_ProductIdAndStatus(
            Long userId, Long productId, ApplicationStatus status);

    // 사용자의 전체 DRAFT 신청 목록 조회 (product fetch join, N+1 방지)
    @Query("SELECT la FROM LoanApplication la " +
           "JOIN FETCH la.product " +
           "WHERE la.user.userId = :userId AND la.status = :status " +
           "ORDER BY la.createdAt DESC")
    List<LoanApplication> findDraftsByUserIdWithProduct(
            @Param("userId") Long userId,
            @Param("status") ApplicationStatus status);

    // 특정 사용자의 EXECUTED 상태 대출 건수 카운트
    int countByUser_UserIdAndStatus(Long userId, ApplicationStatus status);

    // 대출 신청 건의 s_grade_id만 조회
    @Query("SELECT la.sGradeId FROM LoanApplication la WHERE la.applicationId = :applicationId")
    Optional<Long> findSGradeIdByApplicationId(@Param("applicationId") Long applicationId);

    // 지점장 결재 대기 목록 조회: 특정 status, appliedAt 오름차순, User/Product JOIN FETCH
    @Query("SELECT la FROM LoanApplication la " +
           "JOIN FETCH la.user u " +
           "JOIN FETCH la.product p " +
           "WHERE la.status = :status " +
           "ORDER BY la.appliedAt ASC")
    List<LoanApplication> findByStatusWithUserAndProduct(@Param("status") ApplicationStatus status);
    // 상태별 대출 신청 건수 집계 (통계 API용)
    @Query("SELECT la.status AS status, COUNT(la) AS count " +
           "FROM LoanApplication la " +
           "WHERE la.status IN :statuses " +
           "GROUP BY la.status")
    List<StatusCountProjection> countByStatuses(@Param("statuses") List<ApplicationStatus> statuses);

    // 특정 상태의 대출 신청 목록 조회 (배치용)
    List<LoanApplication> findByStatus(ApplicationStatus status);

    // DRAFT 만료 처리: 7일 경과한 DRAFT 상태를 EXPIRED로 일괄 변경
    @Modifying
    @Query("UPDATE LoanApplication la SET la.status = :newStatus " +
           "WHERE la.status = :currentStatus AND la.createdAt < :expiredBefore")
    int bulkUpdateStatusByStatusAndCreatedAtBefore(
            @Param("currentStatus") ApplicationStatus currentStatus,
            @Param("newStatus") ApplicationStatus newStatus,
            @Param("expiredBefore") java.time.LocalDateTime expiredBefore);
}
