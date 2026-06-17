package com.sofit.common.repository.loan;

import com.sofit.common.entity.loan.LoanExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanExecutionRepository extends JpaRepository<LoanExecution, Long> {

    @Query("SELECT e FROM LoanExecution e " +
           "JOIN FETCH e.application a " +
           "JOIN FETCH a.product " +
           "WHERE a.applicationId = :applicationId " +
           "AND a.user.id = :userId " +
           "AND a.status = com.sofit.common.entity.loan.enums.ApplicationStatus.EXECUTED")
    Optional<LoanExecution> findByApplicationIdAndUserId(
            @Param("applicationId") Long applicationId,
            @Param("userId") Long userId
    );

    @Query("SELECT e FROM LoanExecution e " +
           "WHERE e.application.applicationId = :applicationId")
    Optional<LoanExecution> findByApplicationId(@Param("applicationId") Long applicationId);

    @Query("SELECT e FROM LoanExecution e " +
           "JOIN FETCH e.application a " +
           "JOIN FETCH a.product " +
           "WHERE a.user.userId = :userId " +
           "AND a.status = com.sofit.common.entity.loan.enums.ApplicationStatus.EXECUTED " +
           "ORDER BY e.createdAt DESC")
    List<LoanExecution> findAllByUserId(@Param("userId") Long userId);
}
