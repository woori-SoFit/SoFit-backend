package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanDashboardServiceImpl 단위 테스트")
class LoanDashboardServiceImplTest {

    @InjectMocks
    private LoanDashboardServiceImpl loanDashboardService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("findLoanApplications")
    class FindLoanApplicationsTest {

        @Test
        @DisplayName("상태 필터 없이 전체 조회 시 기본 상태 목록으로 조회한다")
        void shouldUseDefaultStatusesWhenNoFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    null, false, 1L, pageable);

            // then
            assertThat(response.totalCount()).isZero();
            assertThat(response.contents()).isEmpty();
            verify(loanApplicationRepository).findDashboardApplications(anyList(), eq(pageable));
        }

        @Test
        @DisplayName("myOnly=true일 때 본인 담당 건만 조회한다")
        void shouldFilterByBankerIdWhenMyOnlyTrue() {
            // given
            Long currentUserId = 100L;
            Pageable pageable = PageRequest.of(0, 10);
            List<ApplicationStatus> statuses = List.of(ApplicationStatus.SYSTEM_APPROVED);
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(loanApplicationRepository.findDashboardApplicationsByBankerId(
                    eq(statuses), eq(currentUserId), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    statuses, true, currentUserId, pageable);

            // then
            assertThat(response.totalCount()).isZero();
            verify(loanApplicationRepository).findDashboardApplicationsByBankerId(
                    eq(statuses), eq(currentUserId), eq(pageable));
            verify(loanApplicationRepository, never()).findDashboardApplications(anyList(), any());
        }

        @Test
        @DisplayName("결과가 있을 때 businessName과 assigneeName을 매핑한다")
        void shouldMapBusinessNameAndAssigneeName() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

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

            Page<LoanApplication> page = new PageImpl<>(List.of(app), pageable, 1);
            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(page);

            BusinessProfile bp = mock(BusinessProfile.class);
            given(bp.getUser()).willReturn(user);
            given(bp.getBusinessName()).willReturn("길동상회");
            given(bp.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
            given(businessProfileRepository.findByUser_UserIdIn(anyList()))
                    .willReturn(List.of(bp));

            User banker = mock(User.class);
            given(banker.getUserId()).willReturn(50L);
            given(banker.getName()).willReturn("김은행");
            given(userRepository.findAllById(anyList())).willReturn(List.of(banker));

            given(loanDecisionRepository.findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                    anyList(), anyList())).willReturn(Collections.emptyList());

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    null, false, 1L, pageable);

            // then
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).businessName()).isEqualTo("길동상회");
            assertThat(response.contents().get(0).assigneeName()).isEqualTo("김은행");
        }

        @Test
        @DisplayName("assignedBankerId가 null인 항목은 assigneeName이 null이다")
        void shouldReturnNullAssigneeNameWhenBankerIdIsNull() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

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
            given(app.getAssignedBankerId()).willReturn(null);
            given(app.getRequestedAmount()).willReturn(30_000_000L);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            Page<LoanApplication> page = new PageImpl<>(List.of(app), pageable, 1);
            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(page);

            given(businessProfileRepository.findByUser_UserIdIn(anyList()))
                    .willReturn(Collections.emptyList());
            given(loanDecisionRepository.findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                    anyList(), anyList())).willReturn(Collections.emptyList());

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    null, false, 1L, pageable);

            // then
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).assigneeName()).isNull();
            assertThat(response.contents().get(0).businessName()).isNull();
        }

        @Test
        @DisplayName("approvedAmount가 있는 decision이 있으면 매핑한다")
        void shouldMapApprovedAmountFromDecision() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);
            given(user.getName()).willReturn("홍길동");

            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(app.getStatus()).willReturn(ApplicationStatus.APPROVED);
            given(app.getAssignedBankerId()).willReturn(50L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            Page<LoanApplication> page = new PageImpl<>(List.of(app), pageable, 1);
            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(page);

            given(businessProfileRepository.findByUser_UserIdIn(anyList()))
                    .willReturn(Collections.emptyList());

            User banker = mock(User.class);
            given(banker.getUserId()).willReturn(50L);
            given(banker.getName()).willReturn("김은행");
            given(userRepository.findAllById(anyList())).willReturn(List.of(banker));

            // approvedAmount를 가진 LoanDecision
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getApplication()).willReturn(app);
            given(decision.getApprovedAmount()).willReturn(45_000_000L);
            given(loanDecisionRepository.findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                    anyList(), anyList())).willReturn(List.of(decision));

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    null, false, 1L, pageable);

            // then
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).approvedAmount()).isEqualTo(45_000_000L);
        }

        @Test
        @DisplayName("같은 userId에 여러 BusinessProfile이 있을 때 최신 createdAt 기준으로 선택한다")
        void shouldSelectLatestBusinessProfile() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

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
            given(app.getAssignedBankerId()).willReturn(null);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            Page<LoanApplication> page = new PageImpl<>(List.of(app), pageable, 1);
            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(page);

            // 오래된 BP
            BusinessProfile oldBp = mock(BusinessProfile.class);
            given(oldBp.getUser()).willReturn(user);
            given(oldBp.getBusinessName()).willReturn("구 상호명");
            given(oldBp.getCreatedAt()).willReturn(LocalDateTime.of(2024, 1, 1, 0, 0));

            // 최신 BP
            BusinessProfile newBp = mock(BusinessProfile.class);
            given(newBp.getUser()).willReturn(user);
            given(newBp.getBusinessName()).willReturn("신 상호명");
            given(newBp.getCreatedAt()).willReturn(LocalDateTime.of(2025, 5, 1, 0, 0));

            given(businessProfileRepository.findByUser_UserIdIn(anyList()))
                    .willReturn(List.of(oldBp, newBp));

            given(loanDecisionRepository.findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                    anyList(), anyList())).willReturn(Collections.emptyList());

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    null, false, 1L, pageable);

            // then
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).businessName()).isEqualTo("신 상호명");
        }

        @Test
        @DisplayName("명시적 statuses 필터를 전달하면 해당 상태로만 조회한다")
        void shouldUseProvidedStatusFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<ApplicationStatus> statuses = List.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED);
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(loanApplicationRepository.findDashboardApplications(eq(statuses), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    statuses, false, 1L, pageable);

            // then
            assertThat(response.totalCount()).isZero();
            verify(loanApplicationRepository).findDashboardApplications(eq(statuses), eq(pageable));
        }
    }

    @Nested
    @DisplayName("findLoanApplicationDetail")
    class FindLoanApplicationDetailTest {

        @Test
        @DisplayName("존재하지 않는 applicationId로 조회 시 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanDashboardService.findLoanApplicationDetail(999L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("정상 조회 시 상세 정보를 반환한다")
        void shouldReturnDetailResponse() {
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
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));

            BusinessProfile bp = mock(BusinessProfile.class);
            given(bp.getBusinessName()).willReturn("길동상회");
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.of(bp));

            User banker = mock(User.class);
            given(banker.getName()).willReturn("김은행");
            given(userRepository.findById(50L)).willReturn(Optional.of(banker));

            // when
            LoanApplicationDetailResponse response = loanDashboardService.findLoanApplicationDetail(10L);

            // then
            assertThat(response.applicationId()).isEqualTo(10L);
            assertThat(response.applicantName()).isEqualTo("홍길동");
            assertThat(response.businessName()).isEqualTo("길동상회");
            assertThat(response.assigneeName()).isEqualTo("김은행");
        }

        @Test
        @DisplayName("assignedBankerId가 null이면 assigneeName은 null이다")
        void shouldReturnNullAssigneeNameWhenNoBankerAssigned() {
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
            given(app.getAssignedBankerId()).willReturn(null);
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.empty());

            // when
            LoanApplicationDetailResponse response = loanDashboardService.findLoanApplicationDetail(10L);

            // then
            assertThat(response.assigneeName()).isNull();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("BusinessProfile이 없으면 businessName은 null이다")
        void shouldReturnNullBusinessNameWhenProfileNotExists() {
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
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.empty());

            User banker = mock(User.class);
            given(banker.getName()).willReturn("김은행");
            given(userRepository.findById(50L)).willReturn(Optional.of(banker));

            // when
            LoanApplicationDetailResponse response = loanDashboardService.findLoanApplicationDetail(10L);

            // then
            assertThat(response.businessName()).isNull();
            assertThat(response.assigneeName()).isEqualTo("김은행");
        }

        @Test
        @DisplayName("은행원이 존재하지 않으면 assigneeName은 null이다")
        void shouldReturnNullAssigneeNameWhenBankerNotFound() {
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
            given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.empty());
            given(userRepository.findById(50L)).willReturn(Optional.empty());

            // when
            LoanApplicationDetailResponse response = loanDashboardService.findLoanApplicationDetail(10L);

            // then
            assertThat(response.assigneeName()).isNull();
        }

        @Test
        @DisplayName("statuses가 빈 리스트이면 기본 상태 목록으로 조회한다")
        void shouldUseDefaultStatusesWhenEmptyList() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(loanApplicationRepository.findDashboardApplications(anyList(), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                    Collections.emptyList(), false, 1L, pageable);

            // then
            assertThat(response.totalCount()).isZero();
            verify(loanApplicationRepository).findDashboardApplications(anyList(), eq(pageable));
        }
    }
}
