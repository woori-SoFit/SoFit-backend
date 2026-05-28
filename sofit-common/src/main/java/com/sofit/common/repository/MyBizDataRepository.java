package com.sofit.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofit.common.entity.mybiz.MyBizData;

public interface MyBizDataRepository extends JpaRepository<MyBizData, Long> {

    // 사용자의 최신 reference_month 데이터 조회
    Optional<MyBizData> findFirstByUser_UserIdOrderByReferenceMonthDesc(Long userId);

    // 사용자의 특정 reference_month 데이터 조회
    Optional<MyBizData> findByUser_UserIdAndReferenceMonth(Long userId, LocalDate referenceMonth);

    // 사용자의 reference_month 범위 데이터 조회 (오름차순)
    List<MyBizData> findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
            Long userId, LocalDate startMonth, LocalDate endMonth);

    // 사용자의 모든 reference_month만 내림차순 조회 (availableMonths 드롭다운용)
    @Query("SELECT m.referenceMonth FROM MyBizData m WHERE m.user.userId = :userId ORDER BY m.referenceMonth DESC")
    List<LocalDate> findReferenceMonthsByUserId(@Param("userId") Long userId);

    // 사용자의 최신 MyBizData 조회 (reference_month DESC, biz_data_id DESC)
    Optional<MyBizData> findFirstByUser_UserIdOrderByReferenceMonthDescBizDataIdDesc(Long userId);
}
