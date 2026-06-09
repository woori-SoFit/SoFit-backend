package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sofit.common.entity.loan.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanProductRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.notification.event.LoanSubmittedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationServiceImpl 단위 테스트")
class LoanApplicationServiceImplTest {

    @InjectMocks
    private LoanApplicationServiceImpl loanApplicationService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankerAssignmentService bankerAssignmentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long APPLICATION_ID = 100L;

    @Nested
    @DisplayName("createApplication")
    class CreateApplicationTest {

        @Test
        @DisplayName("정상 생성 시 DRAFT 상태의 응답을 반환한다")
        void shouldCreateDraftSuccessfully() {
            // given
            User user = createUser(USER_ID);
            LoanProduct product = createProduct(PRODUCT_ID, ProductStatus.ACTIVE);
            LoanApplicationCreateRequest request = createCreateRequest();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(loanApplicationRepository.existsByUser_UserIdAndProduct_ProductIdAndStatusNotIn(
                    eq(USER_ID), eq(PRODUCT_ID), anyList())).willReturn(false);
            given(loanApplicationRepository.save(any(LoanApplication.class)))
                    .willAnswer(inv -> {
                        LoanApplication app = inv.getArgument(0);
                        ReflectionTestUtils.setField(app, "applicationId", APPLICATION_ID);
                        return app;
                    });

            // when
            LoanApplicationCreateResponse response = loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenUserNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, createCreateRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenProductNotFound() {
            // given
            User user = createUser(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, createCreateRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("상품이 ACTIVE가 아니면 PRODUCT_NOT_ACTIVE 예외를 던진다")
        void shouldThrowWhenProductNotActive() {
            // given
            User user = createUser(USER_ID);
            LoanProduct product = createProduct(PRODUCT_ID, ProductStatus.INACTIVE);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, createCreateRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.PRODUCT_NOT_ACTIVE);
        }

        @Test
        @DisplayName("동일 상품 중복 신청 시 DUPLICATE_APPLICATION 예외를 던진다")
        void shouldThrowWhenDuplicateApplication() {
            // given
            User user = createUser(USER_ID);
            LoanProduct product = createProduct(PRODUCT_ID, ProductStatus.ACTIVE);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(loanApplicationRepository.existsByUser_UserIdAndProduct_ProductIdAndStatusNotIn(
                    eq(USER_ID), eq(PRODUCT_ID), anyList())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, createCreateRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.DUPLICATE_APPLICATION);
        }
    }

    @Nested
    @DisplayName("checkDraft")
    class CheckDraftTest {

        @Test
        @DisplayName("DRAFT가 존재하면 hasDraft=true 응답을 반환한다")
        void shouldReturnTrueWhenDraftExists() {
            // given
            LoanApplication draft = createDraftApplication();
            given(loanApplicationRepository.findByUser_UserIdAndProduct_ProductIdAndStatus(
                    USER_ID, PRODUCT_ID, ApplicationStatus.DRAFT))
                    .willReturn(Optional.of(draft));

            // when
            DraftCheckResponse response = loanApplicationService.checkDraft(USER_ID, PRODUCT_ID);

            // then
            assertThat(response.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("DRAFT가 없으면 hasDraft=false 응답을 반환한다")
        void shouldReturnFalseWhenNoDraft() {
            // given
            given(loanApplicationRepository.findByUser_UserIdAndProduct_ProductIdAndStatus(
                    USER_ID, PRODUCT_ID, ApplicationStatus.DRAFT))
                    .willReturn(Optional.empty());

            // when
            DraftCheckResponse response = loanApplicationService.checkDraft(USER_ID, PRODUCT_ID);

            // then
            assertThat(response.hasDraft()).isFalse();
        }
    }

    @Nested
    @DisplayName("findDrafts")
    class FindDraftsTest {

        @Test
        @DisplayName("DRAFT 목록이 비어있으면 빈 리스트를 반환한다")
        void shouldReturnEmptyListWhenNoDrafts() {
            // given
            given(loanApplicationRepository.findDraftsByUserIdWithProduct(USER_ID, ApplicationStatus.DRAFT))
                    .willReturn(Collections.emptyList());

            // when
            DraftListResponse response = loanApplicationService.findDrafts(USER_ID);

            // then
            assertThat(response.drafts()).isEmpty();
        }

        @Test
        @DisplayName("DRAFT 목록이 존재하면 상품명과 resumeStep을 포함한 리스트를 반환한다")
        void shouldReturnDraftListWithProductNames() {
            // given
            LoanApplication draft1 = createDraftApplication();
            LoanApplication draft2 = createDraftApplicationWithStep(201L, LastCompletedStep.BIZ_INFO_DONE);

            given(loanApplicationRepository.findDraftsByUserIdWithProduct(USER_ID, ApplicationStatus.DRAFT))
                    .willReturn(List.of(draft1, draft2));

            // when
            DraftListResponse response = loanApplicationService.findDrafts(USER_ID);

            // then
            assertThat(response.drafts()).hasSize(2);
            assertThat(response.drafts().get(0).productName()).isEqualTo("소상공인 대출");
        }
    }

    @Nested
    @DisplayName("getResumeData")
    class GetResumeDataTest {

        @Test
        @DisplayName("존재하지 않는 신청이면 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenApplicationNotFound() {
            // given
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationService.getResumeData(USER_ID, APPLICATION_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("DRAFT가 아닌 상태이면 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenNotDraftStatus() {
            // given
            LoanApplication application = createApplicationWithStatus(ApplicationStatus.SUBMITTED);
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> loanApplicationService.getResumeData(USER_ID, APPLICATION_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("DRAFT 상태이면 이어가기 데이터를 정상 반환한다")
        void shouldReturnResumeDataWhenDraft() {
            // given
            LoanApplication draft = createDraftApplication();
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.of(draft));

            // when
            var response = loanApplicationService.getResumeData(USER_ID, APPLICATION_ID);

            // then
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(response.resumeStep()).isNotNull();
        }
    }

    @Nested
    @DisplayName("submitApplication")
    class SubmitApplicationTest {

        @Test
        @DisplayName("존재하지 않는 신청이면 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenApplicationNotFound() {
            // given
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, createSubmitRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("DRAFT가 아닌 상태이면 APPLICATION_NOT_DRAFT 예외를 던진다")
        void shouldThrowWhenNotDraftStatus() {
            // given
            LoanApplication application = createApplicationWithStatus(ApplicationStatus.SUBMITTED);
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, createSubmitRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_DRAFT);
        }

        @Test
        @DisplayName("정상 제출 시 SUBMITTED 상태로 변경되고 응답을 반환한다")
        void shouldSubmitSuccessfully() {
            // given
            LoanApplication draft = createDraftApplication();
            given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                    .willReturn(Optional.of(draft));
            given(bankerAssignmentService.assignBanker()).willReturn(50L);

            LoanApplicationSubmitRequest request = createSubmitRequest();

            // when
            var response = loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(response.requestedAmount()).isEqualTo(50000000L);
            assertThat(draft.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(draft.getAssignedBankerId()).isEqualTo(50L);
            verify(eventPublisher).publishEvent(any(LoanSubmittedEvent.class));
        }
    }

    @Nested
    @DisplayName("cancelDraftApplication")
    class CancelDraftApplicationTest {

        @Test
        @DisplayName("존재하지 않는 신청이면 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenApplicationNotFound() {
            // given
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationService.cancelDraftApplication(USER_ID, APPLICATION_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("본인 소유가 아니면 APPLICATION_NOT_OWNED 예외를 던진다")
        void shouldThrowWhenNotOwned() {
            // given
            LoanApplication application = createApplicationWithOwner(2L); // 다른 사용자 소유
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> loanApplicationService.cancelDraftApplication(USER_ID, APPLICATION_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_OWNED);
        }

        @Test
        @DisplayName("DRAFT가 아닌 상태이면 APPLICATION_NOT_DRAFT 예외를 던진다")
        void shouldThrowWhenNotDraftStatus() {
            // given
            LoanApplication application = createApplicationWithOwnerAndStatus(USER_ID, ApplicationStatus.SUBMITTED);
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> loanApplicationService.cancelDraftApplication(USER_ID, APPLICATION_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanErrorCode.APPLICATION_NOT_DRAFT);
        }

        @Test
        @DisplayName("정상 취소 시 CANCELLED 상태로 변경된다")
        void shouldCancelSuccessfully() {
            // given
            LoanApplication application = createApplicationWithOwnerAndStatus(USER_ID, ApplicationStatus.DRAFT);
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            loanApplicationService.cancelDraftApplication(USER_ID, APPLICATION_ID);

            // then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }
    }

    // ===== Helper Methods =====

    private User createUser(Long userId) {
        User user = User.createUser("testuser", "hashedPw", "홍길동", "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private LoanProduct createProduct(Long productId, ProductStatus status) {
        try {
            LoanProduct product = newInstance(LoanProduct.class);
            setField(product, "productId", productId);
            setField(product, "productName", "소상공인 대출");
            setField(product, "status", status);
            return product;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanApplicationCreateRequest createCreateRequest() {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        ReflectionTestUtils.setField(request, "annualIncome", "50000000");
        ReflectionTestUtils.setField(request, "creditScore", "750");
        ReflectionTestUtils.setField(request, "incomeType", com.sofit.common.entity.loan.enums.IncomeType.SALARY);
        ReflectionTestUtils.setField(request, "existingLoanAmt", "10000000");
        return request;
    }

    private LoanApplicationSubmitRequest createSubmitRequest() {
        LoanApplicationSubmitRequest request = new LoanApplicationSubmitRequest();
        ReflectionTestUtils.setField(request, "requestedAmount", 50000000L);
        ReflectionTestUtils.setField(request, "requestedTerm", 36);
        ReflectionTestUtils.setField(request, "repaymentMethod", RepaymentMethod.EQUAL_PAYMENT);
        ReflectionTestUtils.setField(request, "purpose", LoanPurpose.WORKING_CAPITAL);
        return request;
    }

    private LoanApplication createDraftApplication() {
        try {
            User user = createUser(USER_ID);
            LoanProduct product = createProduct(PRODUCT_ID, ProductStatus.ACTIVE);

            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "user", user);
            setField(application, "product", product);
            setField(application, "status", ApplicationStatus.DRAFT);
            setField(application, "lastCompletedStep", LastCompletedStep.CONSENT_DONE);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanApplication createApplicationWithStatus(ApplicationStatus status) {
        try {
            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "status", status);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanApplication createApplicationWithOwner(Long ownerId) {
        try {
            User owner = createUser(ownerId);
            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "user", owner);
            setField(application, "status", ApplicationStatus.DRAFT);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanApplication createApplicationWithOwnerAndStatus(Long ownerId, ApplicationStatus status) {
        try {
            User owner = createUser(ownerId);
            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "user", owner);
            setField(application, "status", status);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanApplication createDraftApplicationWithStep(Long appId, LastCompletedStep step) {
        try {
            User user = createUser(USER_ID);
            LoanProduct product = createProduct(PRODUCT_ID, ProductStatus.ACTIVE);

            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", appId);
            setField(application, "user", user);
            setField(application, "product", product);
            setField(application, "status", ApplicationStatus.DRAFT);
            setField(application, "lastCompletedStep", step);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
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
