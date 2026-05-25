package com.sofit.user.domain.mybiz.property;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.converter.MyBizConverter;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.CashFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.IndustryCompareResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;
import net.jqwik.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: Converter 변환 완전성 (Round-trip field preservation)
 *
 * 랜덤 MyBizData 생성 → Converter 변환 → 필수 필드 non-null + yyyy-MM 형식 확인
 *
 * Validates: Requirements 1.2, 1.3, 1.4, 3.3, 4.3
 */
@Label("Feature: mybiz-dashboard, Property 1: Converter 변환 완전성")
class MyBizConverterPropertyTest {

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    @Property(tries = 100)
    @Label("변환된 응답의 모든 필수 필드가 non-null이고 referenceMonth가 yyyy-MM 형식이다")
    void converterPreservesAllRequiredFields(
            @ForAll("validMyBizBaseData") MyBizData baseData,
            @ForAll("validRevenueTrendData") List<MyBizData> revenueTrendData,
            @ForAll("validCashFlowTrendData") List<MyBizData> cashFlowTrendData
    ) {
        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, revenueTrendData, cashFlowTrendData);

        // then - 1. 모든 필수 필드가 non-null
        assertThat(response.referenceMonth()).isNotNull();
        assertThat(response.monthlyRevenue()).isNotNull();
        assertThat(response.monthlyRevenueGrowthRate()).isNotNull();
        assertThat(response.cashFlow()).isNotNull();
        assertThat(response.estimatedProfit()).isNotNull();
        assertThat(response.industryCompare()).isNotNull();
        assertThat(response.revenueTrend()).isNotNull();
        assertThat(response.cashFlowTrend()).isNotNull();
        assertThat(response.naverRating()).isNotNull();
        assertThat(response.reviewCount()).isNotNull();
        assertThat(response.deliveryReorderRate()).isNotNull();
        assertThat(response.deliveryOrderCount()).isNotNull();

        // then - 2. referenceMonth가 yyyy-MM 형식
        assertThat(response.referenceMonth()).matches(YEAR_MONTH_PATTERN.pattern());

        // then - 3. industryCompare 필드가 non-null
        IndustryCompareResponse industryCompare = response.industryCompare();
        assertThat(industryCompare.industrySalesRank()).isNotNull();
        assertThat(industryCompare.industryProfitRank()).isNotNull();
        assertThat(industryCompare.industryStabilityRank()).isNotNull();

        // then - 4. revenueTrend 항목의 referenceMonth와 monthlyRevenue가 non-null
        for (RevenueTrendResponse trend : response.revenueTrend()) {
            assertThat(trend.referenceMonth()).isNotNull();
            assertThat(trend.referenceMonth()).matches(YEAR_MONTH_PATTERN.pattern());
            assertThat(trend.monthlyRevenue()).isNotNull();
        }

        // then - 5. cashFlowTrend 항목의 referenceMonth, monthlyInflow, monthlyOutflow가 non-null
        for (CashFlowTrendResponse trend : response.cashFlowTrend()) {
            assertThat(trend.referenceMonth()).isNotNull();
            assertThat(trend.referenceMonth()).matches(YEAR_MONTH_PATTERN.pattern());
            assertThat(trend.monthlyInflow()).isNotNull();
            assertThat(trend.monthlyOutflow()).isNotNull();
        }
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<MyBizData> validMyBizBaseData() {
        Arbitrary<LocalDate> refMonthArb = validReferenceMonth();
        Arbitrary<Long> revenueArb = Arbitraries.longs().between(0L, 100_000_000L);
        Arbitrary<BigDecimal> growthRateArb = validBigDecimal(-100, 999);
        Arbitrary<Long> cashFlowArb = Arbitraries.longs().between(-50_000_000L, 50_000_000L);
        Arbitrary<Long> profitArb = Arbitraries.longs().between(-50_000_000L, 50_000_000L);
        Arbitrary<Long> inflowArb = Arbitraries.longs().between(0L, 100_000_000L);
        Arbitrary<Long> outflowArb = Arbitraries.longs().between(0L, 100_000_000L);
        Arbitrary<BigDecimal> ratingArb = validBigDecimal(0, 5);
        Arbitrary<Integer> reviewCntArb = Arbitraries.integers().between(0, 10000);
        Arbitrary<BigDecimal> reorderRateArb = validBigDecimal(0, 100);
        Arbitrary<Integer> deliveryCntArb = Arbitraries.integers().between(0, 10000);
        Arbitrary<BigDecimal> salesRankArb = validBigDecimal(0, 100);
        Arbitrary<BigDecimal> profitRankArb = validBigDecimal(0, 100);
        Arbitrary<BigDecimal> stabilityRankArb = validBigDecimal(0, 100);

        // 첫 번째 그룹 (7개)
        Arbitrary<MyBizData> firstGroup = Combinators.combine(
                refMonthArb, revenueArb, growthRateArb, cashFlowArb, profitArb, inflowArb, outflowArb
        ).as((refMonth, revenue, growthRate, cashFlow, profit, inflow, outflow) -> {
            MyBizData data = createMyBizDataInstance();
            ReflectionTestUtils.setField(data, "referenceMonth", refMonth);
            ReflectionTestUtils.setField(data, "monthlyRevenue", revenue);
            ReflectionTestUtils.setField(data, "monthlyRevenueGrowthRate", growthRate);
            ReflectionTestUtils.setField(data, "cashFlow", cashFlow);
            ReflectionTestUtils.setField(data, "estimatedProfit", profit);
            ReflectionTestUtils.setField(data, "monthlyInflow", inflow);
            ReflectionTestUtils.setField(data, "monthlyOutflow", outflow);
            return data;
        });

        // 두 번째 그룹: 나머지 필드 설정
        return Combinators.combine(
                firstGroup, ratingArb, reviewCntArb, reorderRateArb, deliveryCntArb, salesRankArb, profitRankArb
        ).flatAs((data, rating, reviewCnt, reorderRate, deliveryCnt, salesRank, profitRank) ->
                stabilityRankArb.map(stabilityRank -> {
                    ReflectionTestUtils.setField(data, "reviewRating", rating);
                    ReflectionTestUtils.setField(data, "reviewCount", reviewCnt);
                    ReflectionTestUtils.setField(data, "onlineReorderRate", reorderRate);
                    ReflectionTestUtils.setField(data, "deliveryOrderCount", deliveryCnt);
                    ReflectionTestUtils.setField(data, "industrySalesRank", salesRank);
                    ReflectionTestUtils.setField(data, "industryProfitRank", profitRank);
                    ReflectionTestUtils.setField(data, "industryStabilityRank", stabilityRank);
                    return data;
                })
        );
    }

    @Provide
    Arbitrary<List<MyBizData>> validRevenueTrendData() {
        Arbitrary<MyBizData> singleTrendData = Combinators.combine(
                validReferenceMonth(),
                Arbitraries.longs().between(0L, 100_000_000L)
        ).as((refMonth, revenue) -> {
            MyBizData data = createMyBizDataInstance();
            ReflectionTestUtils.setField(data, "referenceMonth", refMonth);
            ReflectionTestUtils.setField(data, "monthlyRevenue", revenue);
            return data;
        });

        return singleTrendData.list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<MyBizData>> validCashFlowTrendData() {
        Arbitrary<MyBizData> singleTrendData = Combinators.combine(
                validReferenceMonth(),
                Arbitraries.longs().between(0L, 100_000_000L),
                Arbitraries.longs().between(0L, 100_000_000L)
        ).as((refMonth, inflow, outflow) -> {
            MyBizData data = createMyBizDataInstance();
            ReflectionTestUtils.setField(data, "referenceMonth", refMonth);
            ReflectionTestUtils.setField(data, "monthlyInflow", inflow);
            ReflectionTestUtils.setField(data, "monthlyOutflow", outflow);
            return data;
        });

        return singleTrendData.list().ofMinSize(0).ofMaxSize(3);
    }

    // --- 헬퍼 메서드 ---

    private Arbitrary<LocalDate> validReferenceMonth() {
        return Combinators.combine(
                Arbitraries.integers().between(2020, 2025),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> LocalDate.of(year, month, 1));
    }

    private Arbitrary<BigDecimal> validBigDecimal(int min, int max) {
        return Arbitraries.integers().between(min * 100, max * 100)
                .map(i -> BigDecimal.valueOf(i, 2));
    }

    private static MyBizData createMyBizDataInstance() {
        try {
            var constructor = MyBizData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("MyBizData 인스턴스 생성 실패", e);
        }
    }
}
