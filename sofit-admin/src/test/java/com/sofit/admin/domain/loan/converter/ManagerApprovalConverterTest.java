package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalItemResponse;
import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("ManagerApprovalConverter 단위 테스트")
class ManagerApprovalConverterTest {

    @Test
    @DisplayName("빈 리스트 입력 시 빈 applications를 반환한다")
    void shouldReturnEmptyListForEmptyInput() {
        // when
        ManagerApprovalListResponse response = ManagerApprovalConverter
                .toManagerApprovalListResponse(Collections.emptyList(), Map.of(), Map.of());

        // then
        assertThat(response.applications()).isEmpty();
    }

    @Test
    @DisplayName("신청 건 목록을 올바르게 변환한다")
    void shouldConvertApplicationsList() {
        // given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getName()).willReturn("홍길동");

        LoanProduct product = mock(LoanProduct.class);
        given(product.getProductName()).willReturn("소상공인 대출");

        LoanApplication app = mock(LoanApplication.class);
        given(app.getApplicationId()).willReturn(10L);
        given(app.getUser()).willReturn(user);
        given(app.getProduct()).willReturn(product);
        given(app.getAssignedBankerId()).willReturn(50L);
        given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));
        given(app.getRequestedAmount()).willReturn(30_000_000L);

        Map<Long, String> businessNameMap = Map.of(1L, "길동상회");
        Map<Long, String> bankerNameMap = Map.of(50L, "김은행");

        // when
        ManagerApprovalListResponse response = ManagerApprovalConverter
                .toManagerApprovalListResponse(List.of(app), businessNameMap, bankerNameMap);

        // then
        assertThat(response.applications()).hasSize(1);
        ManagerApprovalItemResponse item = response.applications().get(0);
        assertThat(item.id()).isEqualTo(10L);
        assertThat(item.applicationDate()).isEqualTo("2025-06-01");
        assertThat(item.applicantName()).isEqualTo("홍길동");
        assertThat(item.businessName()).isEqualTo("길동상회");
        assertThat(item.productName()).isEqualTo("소상공인 대출");
        assertThat(item.requestedByName()).isEqualTo("김은행");
        assertThat(item.requestedAmount()).isEqualTo(30_000_000L);
    }

    @Test
    @DisplayName("appliedAt이 null이면 applicationDate는 null이다")
    void shouldReturnNullDateWhenAppliedAtIsNull() {
        // given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getName()).willReturn("홍길동");

        LoanProduct product = mock(LoanProduct.class);
        given(product.getProductName()).willReturn("소상공인 대출");

        LoanApplication app = mock(LoanApplication.class);
        given(app.getApplicationId()).willReturn(10L);
        given(app.getUser()).willReturn(user);
        given(app.getProduct()).willReturn(product);
        given(app.getAssignedBankerId()).willReturn(null);
        given(app.getAppliedAt()).willReturn(null);
        given(app.getRequestedAmount()).willReturn(30_000_000L);

        Map<Long, String> businessNameMap = new HashMap<>();
        Map<Long, String> bankerNameMap = new HashMap<>();

        // when
        ManagerApprovalListResponse response = ManagerApprovalConverter
                .toManagerApprovalListResponse(List.of(app), businessNameMap, bankerNameMap);

        // then
        assertThat(response.applications().get(0).applicationDate()).isNull();
        assertThat(response.applications().get(0).businessName()).isNull();
        assertThat(response.applications().get(0).requestedByName()).isNull();
    }
}
