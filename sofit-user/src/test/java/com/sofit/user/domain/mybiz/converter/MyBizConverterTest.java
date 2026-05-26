package com.sofit.user.domain.mybiz.converter;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.CashFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.IndustryCompareResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MyBizConverterTest {

    // --- 헬퍼 메서드 ---

    private MyBizData createMyBizData(LocalDate referenceMonth, Long monthlyRevenue,
                                       BigDecimal monthlyRevenueGrowthRate, Long cashFlow,
                                       Long estimatedProfit, Long monthlyInflow, Long monthlyOutflow,
                                       BigDecimal reviewRating, Integer reviewCount,
                                       BigDecimal onlineReorderRate, Integer deliveryOrderCount,
                                       BigDecimal industrySalesRank, BigDecimal industryProfitRank,
                                       BigDecimal industryStabilityRank) {
        MyBizData data;
        try {
            var constructor = MyBizData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            data = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("MyBizData 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(data, "referenceMonth", referenceMonth);
        ReflectionTestUtils.setField(data, "monthlyRevenue", monthlyRevenue);
        ReflectionTestUtils.setField(data, "prevMonthRevenue", monthlyRevenue);
        ReflectionTestUtils.setField(data, "monthlyRevenueGrowthRate", monthlyRevenueGrowthRate);
        ReflectionTestUtils.setField(data, "cashFlow", cashFlow);
        ReflectionTestUtils.setField(data, "estimatedProfit", estimatedProfit);
        ReflectionTestUtils.setField(data, "monthlyInflow", monthlyInflow);
        ReflectionTestUtils.setField(data, "monthlyOutflow", monthlyOutflow);
        ReflectionTestUtils.setField(data, "reviewRating", reviewRating);
        ReflectionTestUtils.setField(data, "reviewCount", reviewCount);
        ReflectionTestUtils.setField(data, "onlineReorderRate", onlineReorderRate);
        ReflectionTestUtils.setField(data, "deliveryOrderCount", deliveryOrderCount);
        ReflectionTestUtils.setField(data, "industrySalesRank", industrySalesRank);
        ReflectionTestUtils.setField(data, "industryProfitRank", industryProfitRank);
        ReflectionTestUtils.setField(data, "industryStabilityRank", industryStabilityRank);
        return data;
    }

    private MyBizData createBaseData() {
        return createMyBizData(
                LocalDate.of(2024, 5, 1),
                15000000L,
                new BigDecimal("12.50"),
                3000000L,
                2000000L,
                10000000L,
                7000000L,
                new BigDecimal("4.5"),
                120,
                new BigDecimal("35.20"),
                85,
                new BigDecimal("25.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00")
        );
    }

    // --- 테스트 ---

    @Test
    @DisplayName("toMyBizDashboardResponse - 기본 데이터 필드가 정확히 매핑된다")
    void toMyBizDashboardResponse_mapsBaseDataFieldsCorrectly() {
        // given
        MyBizData baseData = createBaseData();
        List<MyBizData> revenueTrend = Collections.emptyList();
        List<MyBizData> cashFlowTrend = Collections.emptyList();

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, revenueTrend, cashFlowTrend, List.of());

        // then
        assertThat(response.monthlyRevenue()).isEqualTo(15000000L);
        assertThat(response.monthlyRevenueGrowthRate()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.cashFlow()).isEqualTo(3000000L);
        assertThat(response.estimatedProfit()).isEqualTo(2000000L);
        assertThat(response.reviewRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(response.reviewCount()).isEqualTo(120);
        assertThat(response.onlineReorderRate()).isEqualByComparingTo(new BigDecimal("35.20"));
        assertThat(response.deliveryOrderCount()).isEqualTo(85);
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - referenceMonth가 yyyy-MM 형식으로 변환된다")
    void toMyBizDashboardResponse_formatsReferenceMonthAsYearMonth() {
        // given
        MyBizData baseData = createBaseData(); // referenceMonth = 2024-05-01

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), Collections.emptyList(), List.of());

        // then
        assertThat(response.referenceMonth()).isEqualTo("2024-05");
        assertThat(response.referenceMonth()).matches("\\d{4}-\\d{2}");
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - revenueTrend 리스트가 정확히 변환된다")
    void toMyBizDashboardResponse_mapsRevenueTrendCorrectly() {
        // given
        MyBizData baseData = createBaseData();

        MyBizData trend1 = createMyBizData(
                LocalDate.of(2024, 3, 1), 12000000L, null, null, null,
                null, null, null, null, null, null, null, null, null);
        MyBizData trend2 = createMyBizData(
                LocalDate.of(2024, 4, 1), 13500000L, null, null, null,
                null, null, null, null, null, null, null, null, null);
        MyBizData trend3 = createMyBizData(
                LocalDate.of(2024, 5, 1), 15000000L, null, null, null,
                null, null, null, null, null, null, null, null, null);

        List<MyBizData> revenueTrendData = List.of(trend1, trend2, trend3);

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, revenueTrendData, Collections.emptyList(), List.of());

        // then
        List<RevenueTrendResponse> revenueTrend = response.revenueTrend();
        assertThat(revenueTrend).hasSize(3);

        assertThat(revenueTrend.get(0).referenceMonth()).isEqualTo("2024-03");
        assertThat(revenueTrend.get(0).monthlyRevenue()).isEqualTo(12000000L);

        assertThat(revenueTrend.get(1).referenceMonth()).isEqualTo("2024-04");
        assertThat(revenueTrend.get(1).monthlyRevenue()).isEqualTo(13500000L);

        assertThat(revenueTrend.get(2).referenceMonth()).isEqualTo("2024-05");
        assertThat(revenueTrend.get(2).monthlyRevenue()).isEqualTo(15000000L);
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - cashFlowTrend 리스트가 정확히 변환된다")
    void toMyBizDashboardResponse_mapsCashFlowTrendCorrectly() {
        // given
        MyBizData baseData = createBaseData();

        MyBizData cashFlow1 = createMyBizData(
                LocalDate.of(2024, 3, 1), null, null, null, null,
                8000000L, 6000000L, null, null, null, null, null, null, null);
        MyBizData cashFlow2 = createMyBizData(
                LocalDate.of(2024, 4, 1), null, null, null, null,
                9000000L, 6500000L, null, null, null, null, null, null, null);
        MyBizData cashFlow3 = createMyBizData(
                LocalDate.of(2024, 5, 1), null, null, null, null,
                10000000L, 7000000L, null, null, null, null, null, null, null);

        List<MyBizData> cashFlowTrendData = List.of(cashFlow1, cashFlow2, cashFlow3);

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), cashFlowTrendData, List.of());

        // then
        List<CashFlowTrendResponse> cashFlowTrend = response.cashFlowTrend();
        assertThat(cashFlowTrend).hasSize(3);

        assertThat(cashFlowTrend.get(0).referenceMonth()).isEqualTo("2024-03");
        assertThat(cashFlowTrend.get(0).monthlyInflow()).isEqualTo(8000000L);
        assertThat(cashFlowTrend.get(0).monthlyOutflow()).isEqualTo(6000000L);

        assertThat(cashFlowTrend.get(1).referenceMonth()).isEqualTo("2024-04");
        assertThat(cashFlowTrend.get(1).monthlyInflow()).isEqualTo(9000000L);
        assertThat(cashFlowTrend.get(1).monthlyOutflow()).isEqualTo(6500000L);

        assertThat(cashFlowTrend.get(2).referenceMonth()).isEqualTo("2024-05");
        assertThat(cashFlowTrend.get(2).monthlyInflow()).isEqualTo(10000000L);
        assertThat(cashFlowTrend.get(2).monthlyOutflow()).isEqualTo(7000000L);
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - industryCompare 필드가 정확히 매핑된다")
    void toMyBizDashboardResponse_mapsIndustryCompareCorrectly() {
        // given
        MyBizData baseData = createBaseData();

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), Collections.emptyList(), List.of());

        // then
        IndustryCompareResponse industryCompare = response.industryCompare();
        assertThat(industryCompare).isNotNull();
        assertThat(industryCompare.industrySalesRank()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(industryCompare.industryProfitRank()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(industryCompare.industryStabilityRank()).isEqualByComparingTo(new BigDecimal("20.00"));
    }
}
