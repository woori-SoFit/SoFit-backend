package com.sofit.common.repository.mybiz;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofit.common.entity.mybiz.MyBizData;

public interface MyBizDataRepository extends JpaRepository<MyBizData, Long> {

    // 사업자번호의 최신 reference_month 데이터 조회
    Optional<MyBizData> findFirstByBusinessNumberOrderByReferenceMonthDesc(String businessNumber);

    // 사업자번호의 특정 reference_month 데이터 조회
    Optional<MyBizData> findByBusinessNumberAndReferenceMonth(String businessNumber, LocalDate referenceMonth);

    // 사업자번호의 reference_month 범위 데이터 조회 (오름차순)
    List<MyBizData> findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
            String businessNumber, LocalDate startMonth, LocalDate endMonth);

    // 사업자번호의 모든 reference_month만 내림차순 조회 (availableMonths 드롭다운용)
    @Query("SELECT m.referenceMonth FROM MyBizData m WHERE m.businessNumber = :businessNumber ORDER BY m.referenceMonth DESC")
    List<LocalDate> findReferenceMonthsByBusinessNumber(@Param("businessNumber") String businessNumber);
}
