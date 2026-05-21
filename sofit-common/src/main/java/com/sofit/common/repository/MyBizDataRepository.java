package com.sofit.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.mybiz.MyBizData;

public interface MyBizDataRepository extends JpaRepository<MyBizData, Long> {

    // 사용자의 최신 reference_month 데이터 조회
    Optional<MyBizData> findFirstByUser_UserIdOrderByReferenceMonthDesc(Long userId);

    // 사용자의 특정 reference_month 데이터 조회
    Optional<MyBizData> findByUser_UserIdAndReferenceMonth(Long userId, LocalDate referenceMonth);

    // 사용자의 reference_month 범위 데이터 조회 (오름차순)
    List<MyBizData> findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
            Long userId, LocalDate startMonth, LocalDate endMonth);
}
