package com.sofit.user.domain.mybiz.converter;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.PaymentFlowTrendResponse;
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
                                       Long monthlyOutflow, Long estimatedProfit,
                                       BigDecimal reviewRating, Integer reviewCount,
                                       BigDecimal onlineReorderRate, Integer deliveryOrderCount,
                                       BigDecimal industrySalesRank, BigDecimal industryProfitRank,
                                       BigDecimal industrySatisfactionRank) {
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
        ReflectionTestUtils.setField(data, "monthlyOutflow", monthlyOutflow);
        ReflectionTestUtils.setField(data, "estimatedProfit", estimatedProfit);
        ReflectionTestUtils.setField(data, "reviewRating", reviewRating);
        ReflectionTestUtils.setField(data, "reviewCount", reviewCount);
        ReflectionTestUtils.setField(data, "onlineReorderRate", onlineReorderRate);
        ReflectionTestUtils.setField(data, "deliveryOrderCount", deliveryOrderCount);
        ReflectionTestUtils.setField(data, "industrySalesRank", industrySalesRank);
        ReflectionTestUtils.setField(data, "industryProfitRank", industryProfitRank);
        ReflectionTestUtils.setField(data, "industrySatisfactionRank", industrySatisfactionRank);
        return data;
    }

    private MyBizData createBaseData() {
        MyBizData data = createMyBizData(
                LocalDate.of(2024, 5, 1),
                15000000L,
                7000000L,
                2000000L,
                new BigDecimal("4.5"),
                120,
                new BigDecimal("35.20"),
                85,
                new BigDecimal("25.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00")
        );
        ReflectionTestUtils.setField(data, "prevMonthRevenue", 13000000L);
        ReflectionTestUtils.setField(data, "monthlyProfitGrowthRate", new BigDecimal("5.50"));
        return data;
    }

    // --- 테스트 ---

    @Test
    @DisplayName("toMyBizDashboardResponse - 기본 데이터 필드가 정확히 매핑된다")
    void toMyBizDashboardResponse_mapsBaseDataFieldsCorrectly() {
        // given
        MyBizData baseData = createBaseData();
        List<MyBizData> sixMonthTrend = Collections.emptyList();
        List<LocalDate> availableMonths = List.of(LocalDate.of(2024, 5, 1));
        BigDecimal monthlyRevenueGrowthRate = new BigDecimal("15.38");

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, sixMonthTrend, availableMonths, monthlyRevenueGrowthRate);

        // then
        assertThat(response.monthlyRevenue()).isEqualTo(15000000L);
        assertThat(response.monthlyRevenueGrowthRate()).isEqualByComparingTo(new BigDecimal("15.38"));
        assertThat(response.estimatedProfit()).isEqualTo(2000000L);
        assertThat(response.reviewRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(response.reviewCount()).isEqualTo(120);
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - referenceMonth가 yyyy-MM 형식으로 변환된다")
    void toMyBizDashboardResponse_formatsReferenceMonthAsYearMonth() {
        // given
        MyBizData baseData = createBaseData(); // referenceMonth = 2024-05-01
        List<LocalDate> availableMonths = List.of(LocalDate.of(2024, 5, 1));

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), availableMonths, null);

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
                LocalDate.of(2024, 3, 1), 12000000L, null, null,
                null, null, null, null, null, null, null);
        MyBizData trend2 = createMyBizData(
                LocalDate.of(2024, 4, 1), 13500000L, null, null,
                null, null, null, null, null, null, null);
        MyBizData trend3 = createMyBizData(
                LocalDate.of(2024, 5, 1), 15000000L, null, null,
                null, null, null, null, null, null, null);

        List<MyBizData> sixMonthTrendData = List.of(trend1, trend2, trend3);
        List<LocalDate> availableMonths = List.of(LocalDate.of(2024, 5, 1));

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, sixMonthTrendData, availableMonths, null);

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
    @DisplayName("toMyBizDashboardResponse - paymentFlowTrend 리스트가 정확히 변환된다")
    void toMyBizDashboardResponse_mapsPaymentFlowTrendCorrectly() {
        // given
        MyBizData baseData = createBaseData();

        MyBizData flow1 = createMyBizData(
                LocalDate.of(2024, 3, 1), 8000000L, 6000000L, 2000000L,
                null, null, null, null, null, null, null);
        MyBizData flow2 = createMyBizData(
                LocalDate.of(2024, 4, 1), 9000000L, 6500000L, 2500000L,
                null, null, null, null, null, null, null);
        MyBizData flow3 = createMyBizData(
                LocalDate.of(2024, 5, 1), 10000000L, 7000000L, 3000000L,
                null, null, null, null, null, null, null);

        List<MyBizData> sixMonthTrendData = List.of(flow1, flow2, flow3);
        List<LocalDate> availableMonths = List.of(LocalDate.of(2024, 5, 1));

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, sixMonthTrendData, availableMonths, null);

        // then
        List<PaymentFlowTrendResponse> paymentFlowTrend = response.paymentFlowTrend();
        assertThat(paymentFlowTrend).hasSize(3);

        assertThat(paymentFlowTrend.get(0).referenceMonth()).isEqualTo("2024-03");
        assertThat(paymentFlowTrend.get(0).monthlyRevenue()).isEqualTo(8000000L);
        assertThat(paymentFlowTrend.get(0).monthlyOutflow()).isEqualTo(6000000L);
        assertThat(paymentFlowTrend.get(0).estimatedProfit()).isEqualTo(2000000L);

        assertThat(paymentFlowTrend.get(1).referenceMonth()).isEqualTo("2024-04");
        assertThat(paymentFlowTrend.get(1).monthlyRevenue()).isEqualTo(9000000L);
        assertThat(paymentFlowTrend.get(1).monthlyOutflow()).isEqualTo(6500000L);
        assertThat(paymentFlowTrend.get(1).estimatedProfit()).isEqualTo(2500000L);

        assertThat(paymentFlowTrend.get(2).referenceMonth()).isEqualTo("2024-05");
        assertThat(paymentFlowTrend.get(2).monthlyRevenue()).isEqualTo(10000000L);
        assertThat(paymentFlowTrend.get(2).monthlyOutflow()).isEqualTo(7000000L);
        assertThat(paymentFlowTrend.get(2).estimatedProfit()).isEqualTo(3000000L);
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - 업종/상권 비교 필드가 정확히 매핑된다")
    void toMyBizDashboardResponse_mapsIndustryCompareCorrectly() {
        // given
        MyBizData baseData = createBaseData();
        List<LocalDate> availableMonths = List.of(LocalDate.of(2024, 5, 1));

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), availableMonths, null);

        // then
        assertThat(response.industrySalesRank()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.industryProfitRank()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(response.industrySatisfactionRank()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("calculateMonthlyRevenueGrowthRate - 정상 계산")
    void calculateMonthlyRevenueGrowthRate_returnsCorrectValue() {
        // when
        BigDecimal result = MyBizConverter.calculateMonthlyRevenueGrowthRate(11000000L, 10000000L);

        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("calculateMonthlyRevenueGrowthRate - prevMonthRevenue가 null이면 null 반환")
    void calculateMonthlyRevenueGrowthRate_returnsNullWhenPrevIsNull() {
        // when
        BigDecimal result = MyBizConverter.calculateMonthlyRevenueGrowthRate(11000000L, null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("calculateMonthlyRevenueGrowthRate - prevMonthRevenue가 0이면 null 반환")
    void calculateMonthlyRevenueGrowthRate_returnsNullWhenPrevIsZero() {
        // when
        BigDecimal result = MyBizConverter.calculateMonthlyRevenueGrowthRate(11000000L, 0L);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toMyBizDashboardResponse - availableMonths가 정확히 변환된다")
    void toMyBizDashboardResponse_mapsAvailableMonthsCorrectly() {
        // given
        MyBizData baseData = createBaseData();
        List<LocalDate> availableMonths = List.of(
                LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 3, 1)
        );

        // when
        MyBizDashboardResponse response = MyBizConverter.toMyBizDashboardResponse(
                baseData, Collections.emptyList(), availableMonths, null);

        // then
        assertThat(response.availableMonths()).containsExactly("2024-05", "2024-04", "2024-03");
    }
}
