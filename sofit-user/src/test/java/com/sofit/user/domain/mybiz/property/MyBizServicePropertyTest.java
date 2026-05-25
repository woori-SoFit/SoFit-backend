package com.sofit.user.domain.mybiz.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mockito.Mockito;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.MyBizDataRepository;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.CashFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;
import com.sofit.user.domain.mybiz.service.MyBizServiceImpl;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

/**
 * MyBiz Dashboard Service 프로퍼티 기반 테스트
 *
 * Property 3: 잘못된 month 형식 거부
 * Property 4: revenueTrend 범위 및 정렬 불변식
 * Property 5: cashFlowTrend 범위 및 정렬 불변식
 */
class MyBizServicePropertyTest {

    private MyBizDataRepository myBizDataRepository;
    private MyBizServiceImpl myBizService;

    private static final Long USER_ID = 1L;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern VALID_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    @BeforeProperty
    void setUp() {
        myBizDataRepository = Mockito.mock(MyBizDataRepository.class);
        myBizService = new MyBizServiceImpl(myBizDataRepository);
    }

    /**
     * Property 3: 잘못된 month 형식 거부
     *
     * yyyy-MM 정규식에 매칭되지 않는 비어있지 않은 문자열을 month 파라미터로 전달하면,
     * 서비스는 항상 BaseException(GeneralErrorCode.BAD_REQUEST)을 발생시켜야 한다.
     *
     * **Validates: Requirements 2.3**
     */
    @Property(tries = 100)
    @Label("Feature: mybiz-dashboard, Property 3: 잘못된 month 형식 거부")
    void invalidMonthFormat_shouldAlwaysThrowBadRequest(
            @ForAll("invalidMonthStrings") String invalidMonth) {

        // when & then
        assertThatThrownBy(() -> myBizService.findDashboard(USER_ID, invalidMonth))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST);
                });
    }

    /**
     * 잘못된 month 형식 문자열을 생성하는 Arbitrary.
     * 모든 생성된 문자열은 yyyy-MM 정규식에 매칭되지 않아야 하며, 비어있지 않아야 한다.
     */
    @Provide
    Arbitrary<String> invalidMonthStrings() {
        Arbitrary<String> alphaStrings = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);

        Arbitrary<String> invalidMonthValues = Arbitraries.of(
                "2024-13", "2024-00", "2024-99", "2024-1", "2024-001"
        );

        Arbitrary<String> invalidSeparators = Arbitraries.of(
                "2024/05", "2024.05", "202405", "2024 05", "2024_05"
        );

        Arbitrary<String> shortYears = Arbitraries.of(
                "24-05", "999-05", "12345-05"
        );

        Arbitrary<String> miscInvalid = Arbitraries.of(
                "abcd-ef", "----", "2024-", "-05", "2024-5",
                "20240501", "2024/01/01", "not-a-date", "2024-0a"
        );

        Arbitrary<String> randomStrings = Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(15);

        return Arbitraries.oneOf(
                alphaStrings,
                invalidMonthValues,
                invalidSeparators,
                shortYears,
                miscInvalid,
                randomStrings
        ).filter(s -> s != null && !s.isBlank() && !VALID_MONTH_PATTERN.matcher(s).matches());
    }

    /**
     * Property 4: revenueTrend 범위 및 정렬 불변식
     *
     * 랜덤 데이터 목록 + 기준월 → revenueTrend 크기(0~5)/범위/오름차순 정렬 확인
     *
     * **Validates: Requirements 3.1, 3.2, 3.4, 3.5**
     */
    @Property(tries = 100)
    @Label("Feature: mybiz-dashboard, Property 4: revenueTrend 범위 및 정렬 불변식")
    void revenueTrend_shouldSatisfyRangeAndSortingInvariants(
            @ForAll("referenceMonths") LocalDate referenceMonth,
            @ForAll("myBizDataLists") List<MyBizData> allData
    ) {
        // 기준월 데이터 생성
        MyBizData baseData = createMyBizData(referenceMonth);

        // revenueTrend 범위: 기준월 포함 이전 5개월 (referenceMonth - 4 months ~ referenceMonth)
        LocalDate revenueStart = referenceMonth.minusMonths(4);
        List<MyBizData> revenueTrendData = allData.stream()
                .filter(d -> !d.getReferenceMonth().isBefore(revenueStart)
                        && !d.getReferenceMonth().isAfter(referenceMonth))
                .sorted(Comparator.comparing(MyBizData::getReferenceMonth))
                .collect(Collectors.toList());

        // cashFlowTrend 범위: 기준월 포함 이전 3개월 (referenceMonth - 2 months ~ referenceMonth)
        LocalDate cashFlowStart = referenceMonth.minusMonths(2);
        List<MyBizData> cashFlowTrendData = allData.stream()
                .filter(d -> !d.getReferenceMonth().isBefore(cashFlowStart)
                        && !d.getReferenceMonth().isAfter(referenceMonth))
                .sorted(Comparator.comparing(MyBizData::getReferenceMonth))
                .collect(Collectors.toList());

        // Mock 설정
        given(myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID))
                .willReturn(Optional.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                eq(USER_ID), eq(revenueStart), eq(referenceMonth)))
                .willReturn(revenueTrendData);
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                eq(USER_ID), eq(cashFlowStart), eq(referenceMonth)))
                .willReturn(cashFlowTrendData);

        // 서비스 호출
        MyBizDashboardResponse response = myBizService.findDashboard(USER_ID, null);

        // 검증
        List<RevenueTrendResponse> revenueTrend = response.revenueTrend();

        // 검증 1: revenueTrend 크기는 0~5 범위
        assertThat(revenueTrend.size()).isBetween(0, 5);

        // 검증 2: 모든 revenueTrend 항목의 referenceMonth가 범위 내에 있음
        String startMonthStr = revenueStart.format(YEAR_MONTH_FORMATTER);
        String endMonthStr = referenceMonth.format(YEAR_MONTH_FORMATTER);

        for (RevenueTrendResponse item : revenueTrend) {
            assertThat(item.referenceMonth()).isGreaterThanOrEqualTo(startMonthStr);
            assertThat(item.referenceMonth()).isLessThanOrEqualTo(endMonthStr);
        }

        // 검증 3: revenueTrend 항목이 referenceMonth 오름차순으로 정렬되어 있음
        for (int i = 1; i < revenueTrend.size(); i++) {
            assertThat(revenueTrend.get(i).referenceMonth())
                    .isGreaterThanOrEqualTo(revenueTrend.get(i - 1).referenceMonth());
        }
    }

    /**
     * Property 5: cashFlowTrend 범위 및 정렬 불변식
     *
     * 랜덤 데이터 목록 + 기준월 → cashFlowTrend 크기(0~3)/범위/오름차순 정렬 확인
     *
     * **Validates: Requirements 4.1, 4.2, 4.4, 4.5**
     */
    @Property(tries = 100)
    @Label("Feature: mybiz-dashboard, Property 5: cashFlowTrend 범위 및 정렬 불변식")
    void cashFlowTrend_shouldSatisfyRangeAndSortingInvariants(
            @ForAll("referenceMonths") LocalDate referenceMonth,
            @ForAll("myBizDataLists") List<MyBizData> allData
    ) {
        // 기준월 데이터 생성
        MyBizData baseData = createMyBizData(referenceMonth);

        // cashFlowTrend 범위: 기준월 포함 이전 3개월 (referenceMonth - 2 months ~ referenceMonth)
        LocalDate cashFlowStart = referenceMonth.minusMonths(2);
        List<MyBizData> cashFlowTrendData = allData.stream()
                .filter(d -> !d.getReferenceMonth().isBefore(cashFlowStart)
                        && !d.getReferenceMonth().isAfter(referenceMonth))
                .sorted(Comparator.comparing(MyBizData::getReferenceMonth))
                .collect(Collectors.toList());

        // revenueTrend 범위: 기준월 포함 이전 5개월 (referenceMonth - 4 months ~ referenceMonth)
        LocalDate revenueStart = referenceMonth.minusMonths(4);
        List<MyBizData> revenueTrendData = allData.stream()
                .filter(d -> !d.getReferenceMonth().isBefore(revenueStart)
                        && !d.getReferenceMonth().isAfter(referenceMonth))
                .sorted(Comparator.comparing(MyBizData::getReferenceMonth))
                .collect(Collectors.toList());

        // Mock 설정
        given(myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID))
                .willReturn(Optional.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                eq(USER_ID), eq(revenueStart), eq(referenceMonth)))
                .willReturn(revenueTrendData);
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                eq(USER_ID), eq(cashFlowStart), eq(referenceMonth)))
                .willReturn(cashFlowTrendData);

        // 서비스 호출
        MyBizDashboardResponse response = myBizService.findDashboard(USER_ID, null);

        // 검증
        List<CashFlowTrendResponse> cashFlowTrend = response.cashFlowTrend();

        // 검증 1: cashFlowTrend 크기는 0~3 범위
        assertThat(cashFlowTrend.size()).isBetween(0, 3);

        // 검증 2: 모든 cashFlowTrend 항목의 referenceMonth가 범위 내에 있음
        String startMonthStr = cashFlowStart.format(YEAR_MONTH_FORMATTER);
        String endMonthStr = referenceMonth.format(YEAR_MONTH_FORMATTER);

        for (CashFlowTrendResponse item : cashFlowTrend) {
            assertThat(item.referenceMonth()).isGreaterThanOrEqualTo(startMonthStr);
            assertThat(item.referenceMonth()).isLessThanOrEqualTo(endMonthStr);
        }

        // 검증 3: cashFlowTrend 항목이 referenceMonth 오름차순으로 정렬되어 있음
        for (int i = 1; i < cashFlowTrend.size(); i++) {
            assertThat(cashFlowTrend.get(i).referenceMonth())
                    .isGreaterThanOrEqualTo(cashFlowTrend.get(i - 1).referenceMonth());
        }
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<LocalDate> referenceMonths() {
        // 2020-01 ~ 2025-12 범위의 월 1일 날짜 생성
        return Arbitraries.integers().between(2020, 2025).flatMap(year ->
                Arbitraries.integers().between(1, 12).map(month ->
                        LocalDate.of(year, month, 1)
                )
        );
    }

    @Provide
    Arbitrary<List<MyBizData>> myBizDataLists() {
        // 0~10개의 랜덤 MyBizData 엔티티 리스트 생성
        return referenceMonths().list().ofMinSize(0).ofMaxSize(10)
                .map(dates -> dates.stream()
                        .map(this::createMyBizData)
                        .collect(Collectors.toList()));
    }

    // --- Helper Methods ---

    private MyBizData createMyBizData(LocalDate referenceMonth) {
        try {
            var constructor = MyBizData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MyBizData data = constructor.newInstance();

            setField(data, "bizDataId", (long) (referenceMonth.getYear() * 100 + referenceMonth.getMonthValue()));
            setField(data, "referenceMonth", referenceMonth);
            setField(data, "monthlyRevenue", 10_000_000L);
            setField(data, "prevMonthRevenue", 9_000_000L);
            setField(data, "monthlyRevenueGrowthRate", new BigDecimal("11.11"));
            setField(data, "monthlyInflow", 12_000_000L);
            setField(data, "monthlyOutflow", 8_000_000L);
            setField(data, "estimatedProfit", 2_000_000L);
            setField(data, "cashFlow", 4_000_000L);
            setField(data, "deliveryOrderCount", 150);
            setField(data, "onlineReorderRate", new BigDecimal("35.50"));
            setField(data, "reviewRating", new BigDecimal("4.5"));
            setField(data, "reviewCount", 120);
            setField(data, "industrySalesRank", new BigDecimal("25.00"));
            setField(data, "industryProfitRank", new BigDecimal("30.00"));
            setField(data, "industryStabilityRank", new BigDecimal("20.00"));
            setField(data, "businessNumber", "1234567890");

            return data;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
