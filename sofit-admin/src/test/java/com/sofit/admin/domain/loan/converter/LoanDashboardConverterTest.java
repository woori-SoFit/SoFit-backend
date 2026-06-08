package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationItemResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("LoanDashboardConverter 단위 테스트")
class LoanDashboardConverterTest {

    @Nested
    @DisplayName("toLoanDashboardResponse")
    class ToDashboardResponseTest {

        @Test
        @DisplayName("빈 페이지를 변환하면 빈 contents를 반환한다")
        void shouldReturnEmptyContentsForEmptyPage() {
            // given
            Page<LoanApplication> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0);

            // when
            LoanDashboardResponse response = LoanDashboardConverter
                    .toLoanDashboardResponse(emptyPage, Map.of(), Map.of(), Map.of());

            // then
            assertThat(response.totalCount()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.currentPage()).isZero();
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.contents()).isEmpty();
        }

        @Test
        @DisplayName("페이지 데이터를 올바르게 변환한다")
        void shouldConvertPageData() {
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
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(50L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            Page<LoanApplication> page = new PageImpl<>(
                    List.of(app), PageRequest.of(0, 10), 1);

            Map<Long, String> businessNameMap = Map.of(1L, "길동상회");
            Map<Long, String> bankerNameMap = Map.of(50L, "김은행");
            Map<Long, Long> approvedAmountMap = Map.of(10L, 45_000_000L);

            // when
            LoanDashboardResponse response = LoanDashboardConverter
                    .toLoanDashboardResponse(page, businessNameMap, bankerNameMap, approvedAmountMap);

            // then
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.contents()).hasSize(1);

            LoanApplicationItemResponse item = response.contents().get(0);
            assertThat(item.applicationId()).isEqualTo(10L);
            assertThat(item.applicantName()).isEqualTo("홍길동");
            assertThat(item.businessName()).isEqualTo("길동상회");
            assertThat(item.productName()).isEqualTo("소상공인 대출");
            assertThat(item.assigneeName()).isEqualTo("김은행");
            assertThat(item.requestedAmount()).isEqualTo(50_000_000L);
            assertThat(item.approvedAmount()).isEqualTo(45_000_000L);
        }
    }

    @Nested
    @DisplayName("toLoanApplicationDetailResponse")
    class ToDetailResponseTest {

        @Test
        @DisplayName("상세 정보를 올바르게 변환한다")
        void shouldConvertDetailResponse() {
            // given
            User user = mock(User.class);
            given(user.getName()).willReturn("홍길동");

            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(50L);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0, 0));

            // when
            LoanApplicationDetailResponse response = LoanDashboardConverter
                    .toLoanApplicationDetailResponse(app, "길동상회", "김은행");

            // then
            assertThat(response.applicationId()).isEqualTo(10L);
            assertThat(response.applicantName()).isEqualTo("홍길동");
            assertThat(response.businessName()).isEqualTo("길동상회");
            assertThat(response.productName()).isEqualTo("소상공인 대출");
            assertThat(response.status()).isEqualTo("SYSTEM_APPROVED");
            assertThat(response.appliedAt()).isEqualTo("2025-06-01 10:00:00");
            assertThat(response.assigneeName()).isEqualTo("김은행");
        }

        @Test
        @DisplayName("appliedAt이 null이면 null을 반환한다")
        void shouldReturnNullAppliedAtWhenNull() {
            // given
            User user = mock(User.class);
            given(user.getName()).willReturn("홍길동");

            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(null);
            given(app.getAppliedAt()).willReturn(null);

            // when
            LoanApplicationDetailResponse response = LoanDashboardConverter
                    .toLoanApplicationDetailResponse(app, null, null);

            // then
            assertThat(response.appliedAt()).isNull();
            assertThat(response.businessName()).isNull();
            assertThat(response.assigneeName()).isNull();
        }
    }
}
