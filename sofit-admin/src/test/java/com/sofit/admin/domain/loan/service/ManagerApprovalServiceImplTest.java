package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManagerApprovalServiceImpl 단위 테스트")
class ManagerApprovalServiceImplTest {

    @InjectMocks
    private ManagerApprovalServiceImpl managerApprovalService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("MANAGER_REVIEW 상태의 신청 건이 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoApplications() {
        // given
        given(loanApplicationRepository.findByStatusWithUserAndProduct(ApplicationStatus.MANAGER_REVIEW))
                .willReturn(Collections.emptyList());

        // when
        ManagerApprovalListResponse response = managerApprovalService.findManagerReviewApplications();

        // then
        assertThat(response.applications()).isEmpty();
        verify(businessProfileRepository, never()).findByUser_UserIdIn(anyList());
        verify(userRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("MANAGER_REVIEW 상태의 신청 건이 있으면 businessName과 bankerName을 매핑하여 반환한다")
    void shouldReturnMappedApplications() {
        // given
        User applicant = mock(User.class);
        given(applicant.getUserId()).willReturn(1L);
        given(applicant.getName()).willReturn("홍길동");

        LoanProduct product = mock(LoanProduct.class);
        given(product.getProductName()).willReturn("소상공인 대출");

        LoanApplication app = mock(LoanApplication.class);
        given(app.getApplicationId()).willReturn(10L);
        given(app.getUser()).willReturn(applicant);
        given(app.getProduct()).willReturn(product);
        given(app.getAssignedBankerId()).willReturn(50L);
        given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));
        given(app.getRequestedAmount()).willReturn(30_000_000L);

        given(loanApplicationRepository.findByStatusWithUserAndProduct(ApplicationStatus.MANAGER_REVIEW))
                .willReturn(List.of(app));

        BusinessProfile bp = mock(BusinessProfile.class);
        given(bp.getUser()).willReturn(applicant);
        given(bp.getBusinessName()).willReturn("길동상회");
        given(bp.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
        given(businessProfileRepository.findByUser_UserIdIn(anyList())).willReturn(List.of(bp));

        User banker = mock(User.class);
        given(banker.getUserId()).willReturn(50L);
        given(banker.getName()).willReturn("김은행");
        given(userRepository.findAllById(anyList())).willReturn(List.of(banker));

        // when
        ManagerApprovalListResponse response = managerApprovalService.findManagerReviewApplications();

        // then
        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().get(0).applicantName()).isEqualTo("홍길동");
        assertThat(response.applications().get(0).businessName()).isEqualTo("길동상회");
        assertThat(response.applications().get(0).requestedByName()).isEqualTo("김은행");
        assertThat(response.applications().get(0).productName()).isEqualTo("소상공인 대출");
        assertThat(response.applications().get(0).requestedAmount()).isEqualTo(30_000_000L);
    }

    @Test
    @DisplayName("assignedBankerId가 null인 건은 bankerName이 null로 매핑된다")
    void shouldHandleNullAssignedBankerId() {
        // given
        User applicant = mock(User.class);
        given(applicant.getUserId()).willReturn(1L);
        given(applicant.getName()).willReturn("홍길동");

        LoanProduct product = mock(LoanProduct.class);
        given(product.getProductName()).willReturn("소상공인 대출");

        LoanApplication app = mock(LoanApplication.class);
        given(app.getApplicationId()).willReturn(10L);
        given(app.getUser()).willReturn(applicant);
        given(app.getProduct()).willReturn(product);
        given(app.getAssignedBankerId()).willReturn(null);
        given(app.getAppliedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));
        given(app.getRequestedAmount()).willReturn(30_000_000L);

        given(loanApplicationRepository.findByStatusWithUserAndProduct(ApplicationStatus.MANAGER_REVIEW))
                .willReturn(List.of(app));

        BusinessProfile bp = mock(BusinessProfile.class);
        given(bp.getUser()).willReturn(applicant);
        given(bp.getBusinessName()).willReturn("길동상회");
        given(bp.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
        given(businessProfileRepository.findByUser_UserIdIn(anyList())).willReturn(List.of(bp));

        given(userRepository.findAllById(anyList())).willReturn(Collections.emptyList());

        // when
        ManagerApprovalListResponse response = managerApprovalService.findManagerReviewApplications();

        // then
        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().get(0).requestedByName()).isNull();
    }
}
