package com.sofit.user.domain.mybiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.MyBizDataRepository;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.exception.MyBizErrorCode;

@ExtendWith(MockitoExtension.class)
class MyBizServiceImplTest {

    @InjectMocks
    private MyBizServiceImpl myBizService;

    @Mock
    private MyBizDataRepository myBizDataRepository;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("month=null → 최신 데이터 반환")
    void findDashboard_withNullMonth_returnsLatestData() {
        // given
        LocalDate referenceMonth = LocalDate.of(2024, 5, 1);
        MyBizData baseData = createMyBizData(referenceMonth);

        given(myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID))
                .willReturn(Optional.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(4), referenceMonth))
                .willReturn(List.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(2), referenceMonth))
                .willReturn(List.of(baseData));

        // when
        MyBizDashboardResponse response = myBizService.findDashboard(USER_ID, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.referenceMonth()).isEqualTo("2024-05");
        verify(myBizDataRepository).findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID);
    }

    @Test
    @DisplayName("month=\"2024-05\" → 해당 월 데이터 반환")
    void findDashboard_withValidMonth_returnsSpecificMonthData() {
        // given
        LocalDate referenceMonth = LocalDate.of(2024, 5, 1);
        MyBizData baseData = createMyBizData(referenceMonth);

        given(myBizDataRepository.findByUser_UserIdAndReferenceMonth(USER_ID, referenceMonth))
                .willReturn(Optional.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(4), referenceMonth))
                .willReturn(List.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(2), referenceMonth))
                .willReturn(List.of(baseData));

        // when
        MyBizDashboardResponse response = myBizService.findDashboard(USER_ID, "2024-05");

        // then
        assertThat(response).isNotNull();
        assertThat(response.referenceMonth()).isEqualTo("2024-05");
        verify(myBizDataRepository).findByUser_UserIdAndReferenceMonth(USER_ID, referenceMonth);
    }

    @Test
    @DisplayName("month=\"\" → 최신 데이터 반환 (빈 문자열)")
    void findDashboard_withEmptyMonth_returnsLatestData() {
        // given
        LocalDate referenceMonth = LocalDate.of(2024, 5, 1);
        MyBizData baseData = createMyBizData(referenceMonth);

        given(myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID))
                .willReturn(Optional.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(4), referenceMonth))
                .willReturn(List.of(baseData));
        given(myBizDataRepository.findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                USER_ID, referenceMonth.minusMonths(2), referenceMonth))
                .willReturn(List.of(baseData));

        // when
        MyBizDashboardResponse response = myBizService.findDashboard(USER_ID, "");

        // then
        assertThat(response).isNotNull();
        assertThat(response.referenceMonth()).isEqualTo("2024-05");
        verify(myBizDataRepository).findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID);
    }

    @Test
    @DisplayName("데이터 미존재 → MY_BIZ_DATA_NOT_FOUND 예외")
    void findDashboard_withNoData_throwsMyBizDataNotFoundException() {
        // given
        given(myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDesc(USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> myBizService.findDashboard(USER_ID, null))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(MyBizErrorCode.MY_BIZ_DATA_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("잘못된 month 형식 → BAD_REQUEST 예외")
    void findDashboard_withInvalidMonthFormat_throwsBadRequest() {
        // when & then
        assertThatThrownBy(() -> myBizService.findDashboard(USER_ID, "invalid"))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST);
                });
    }

    /**
     * 리플렉션을 사용하여 MyBizData 테스트 인스턴스를 생성한다.
     * Entity가 protected 생성자만 제공하므로 테스트에서 리플렉션으로 필드를 설정한다.
     */
    private MyBizData createMyBizData(LocalDate referenceMonth) {
        try {
            var constructor = MyBizData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MyBizData data = constructor.newInstance();

            setField(data, "bizDataId", 1L);
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
