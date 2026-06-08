package com.sofit.user.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.sofit.common.entity.sGrade.SGradeHistory;
import com.sofit.common.repository.sGrade.SGradeHistoryRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.user.domain.auth.client.ExternalMockClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.term.ConsentHistoryRepository;
import com.sofit.common.repository.term.TermRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.auth.RegistrationProcessRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.request.SignupCompleteRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;
import com.sofit.user.domain.auth.dto.response.CheckLoginIdResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private ExternalMockClient externalMockClient;
    @Mock private FinancialCertService financialCertService;
    @Mock private UserRepository userRepository;
    @Mock private RegistrationProcessRepository registrationProcessRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private TermRepository termRepository;
    @Mock private ConsentHistoryRepository consentHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HttpSessionSecurityContextRepository securityContextRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private SGradeHistoryRepository sGradeHistoryRepository;

    // ===== checkLoginId 테스트 =====

    @Nested
    @DisplayName("checkLoginId")
    class CheckLoginIdTest {

        @Test
        @DisplayName("사용 가능한 아이디 조회 시 available=true를 반환한다")
        void checkLoginId_사용가능한_아이디_조회시_available_true_반환() {
            // given
            given(userRepository.existsByLoginId("newuser1")).willReturn(false);

            // when
            CheckLoginIdResponse response = authService.checkLoginId("newuser1");

            // then
            assertThat(response.loginId()).isEqualTo("newuser1");
            assertThat(response.available()).isTrue();
        }

        @Test
        @DisplayName("이미 사용 중인 아이디 조회 시 available=false를 반환한다")
        void checkLoginId_이미_사용중인_아이디_조회시_available_false_반환() {
            // given
            given(userRepository.existsByLoginId("existing1")).willReturn(true);

            // when
            CheckLoginIdResponse response = authService.checkLoginId("existing1");

            // then
            assertThat(response.loginId()).isEqualTo("existing1");
            assertThat(response.available()).isFalse();
        }

        @Test
        @DisplayName("아이디 형식이 잘못된 경우 INVALID_LOGIN_ID_FORMAT 예외가 발생한다 - 3자리")
        void checkLoginId_형식_잘못된_경우_INVALID_LOGIN_ID_FORMAT_예외_발생_3자리() {
            // when & then
            assertThatThrownBy(() -> authService.checkLoginId("abc"))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_LOGIN_ID_FORMAT));
        }

        @Test
        @DisplayName("아이디 형식이 잘못된 경우 INVALID_LOGIN_ID_FORMAT 예외가 발생한다 - 특수문자 포함")
        void checkLoginId_형식_잘못된_경우_INVALID_LOGIN_ID_FORMAT_예외_발생_특수문자() {
            // when & then
            assertThatThrownBy(() -> authService.checkLoginId("user@123"))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_LOGIN_ID_FORMAT));
        }

        @Test
        @DisplayName("아이디가 null인 경우 INVALID_LOGIN_ID_FORMAT 예외가 발생한다")
        void checkLoginId_null인_경우_INVALID_LOGIN_ID_FORMAT_예외_발생() {
            // when & then
            assertThatThrownBy(() -> authService.checkLoginId(null))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_LOGIN_ID_FORMAT));
        }

        @Test
        @DisplayName("아이디가 21자 이상인 경우 INVALID_LOGIN_ID_FORMAT 예외가 발생한다")
        void checkLoginId_21자_이상인_경우_INVALID_LOGIN_ID_FORMAT_예외_발생() {
            // when & then
            assertThatThrownBy(() -> authService.checkLoginId("abcdefghijklmnopqrstu"))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_LOGIN_ID_FORMAT));
        }
    }

    // ===== login 테스트 =====

    @Nested
    @DisplayName("login")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 시 LoginResponse를 반환한다")
        void login_정상_로그인시_LoginResponse_반환() {
            // given
            LoginRequest request = createLoginRequest("testuser", "password1!");
            User user = createActiveUser(1L, "testuser", "홍길동");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);
            HttpSession session = mock(HttpSession.class);

            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password1!", user.getPasswordHash())).willReturn(true);
            given(httpRequest.getSession()).willReturn(session);

            // when
            LoginResponse response = authService.login(request, httpRequest, httpResponse);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("존재하지 않는 아이디로 로그인 시 LOGIN_FAILED 예외가 발생한다")
        void login_존재하지_않는_아이디_로그인시_LOGIN_FAILED_예외_발생() {
            // given
            LoginRequest request = createLoginRequest("unknown", "password1!");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.LOGIN_FAILED));
        }

        @Test
        @DisplayName("탈퇴한 계정으로 로그인 시 ACCOUNT_WITHDRAWN 예외가 발생한다")
        void login_탈퇴한_계정_로그인시_ACCOUNT_WITHDRAWN_예외_발생() {
            // given
            LoginRequest request = createLoginRequest("withdrawn", "password1!");
            User user = createInactiveUser(2L, "withdrawn", "탈퇴자");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(userRepository.findByLoginId("withdrawn")).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.ACCOUNT_WITHDRAWN));
        }

        @Test
        @DisplayName("비밀번호 불일치 시 LOGIN_FAILED 예외가 발생한다")
        void login_비밀번호_불일치시_LOGIN_FAILED_예외_발생() {
            // given
            LoginRequest request = createLoginRequest("testuser", "wrongpassword");
            User user = createActiveUser(1L, "testuser", "홍길동");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);

            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.LOGIN_FAILED));
        }
    }

    // ===== logout 테스트 =====

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("세션이 존재하는 경우 세션을 무효화한다")
        void logout_세션_존재시_세션_무효화() {
            // given
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            given(httpRequest.getSession(false)).willReturn(session);

            // when
            authService.logout(httpRequest);

            // then
            verify(session).invalidate();
        }

        @Test
        @DisplayName("세션이 없는 경우 예외 없이 정상 처리된다")
        void logout_세션_없는_경우_예외없이_정상_처리() {
            // given
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            given(httpRequest.getSession(false)).willReturn(null);

            // when & then (예외 없이 정상 종료)
            authService.logout(httpRequest);
            verify(httpRequest).getSession(false);
        }
    }

    // ===== verifyBusiness 테스트 =====

    @Nested
    @DisplayName("verifyBusiness")
    class VerifyBusinessTest {

        @Test
        @DisplayName("이미 가입된 사업자번호로 요청 시 BUSINESS_ALREADY_REGISTERED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_이미_가입된_사업자번호_BUSINESS_ALREADY_REGISTERED_예외_발생() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            User activeUser = createActiveUser(1L, "testuser", "홍길동");
            com.sofit.common.entity.auth.BusinessProfile profile =
                    com.sofit.common.entity.auth.BusinessProfile.createVerified(
                            activeUser, "1234567890", "홍길동", "음식점업", "한식",
                            "테스트상호", "서울시", java.time.LocalDate.of(2020, 1, 1));

            // TransactionTemplate.execute() → 콜백 직접 실행
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.of(profile));

            // when & then
            assertThatThrownBy(() -> authService.verifyBusiness(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.BUSINESS_ALREADY_REGISTERED));
        }

        @Test
        @DisplayName("INACTIVE 사용자의 사업자번호로 요청 시 정상 진행된다 (KYC 재호출)")
        @SuppressWarnings("unchecked")
        void verifyBusiness_INACTIVE_사용자_사업자번호_정상_진행() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            User inactiveUser = createInactiveUser(1L, "testuser", "홍길동");
            com.sofit.common.entity.auth.BusinessProfile profile =
                    com.sofit.common.entity.auth.BusinessProfile.createVerified(
                            inactiveUser, "1234567890", "홍길동", "음식점업", "한식",
                            "테스트상호", "서울시", java.time.LocalDate.of(2020, 1, 1));

            ExternalKycResponse validKyc = new ExternalKycResponse(
                    "1234567890", "홍길동", "음식점업", "한식", "테스트상호", "서울시", "2020-01-01", true
            );
            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", validKyc);

            RegistrationProcess savedProcess = createRegistrationProcess(
                    10L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now()
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.of(profile));
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);
            given(registrationProcessRepository.save(any(RegistrationProcess.class)))
                    .willReturn(savedProcess);

            // when
            BusinessVerificationResponse response = authService.verifyBusiness(request, session);

            // then
            assertThat(response).isNotNull();
            assertThat(response.businessNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("KYC 인증 실패 시 BUSINESS_NOT_FOUND 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_KYC_인증_실패시_BUSINESS_NOT_FOUND_예외_발생() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("9999999999");
            HttpSession session = mock(HttpSession.class);

            ExternalMockApiResponse<ExternalKycResponse> failResponse =
                    new ExternalMockApiResponse<>(false, "AUTH4041", "일치하는 사업자등록번호를 찾을 수 없습니다.", null);

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("9999999999"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("9999999999"))
                    .willReturn(Optional.empty());
            given(externalMockClient.callKycVerify("9999999999")).willReturn(failResponse);

            // when & then
            assertThatThrownBy(() -> authService.verifyBusiness(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.BUSINESS_NOT_FOUND));
        }

        @Test
        @DisplayName("KYC isValid=false 시 BUSINESS_NOT_FOUND 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_KYC_isValid_false시_BUSINESS_NOT_FOUND_예외_발생() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            ExternalKycResponse invalidKyc = new ExternalKycResponse(
                    "1234567890", "홍길동", "음식점업", "한식", "테스트상호", "서울시", "2020-01-01", false
            );
            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", invalidKyc);

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> authService.verifyBusiness(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.BUSINESS_NOT_FOUND));
        }

        @Test
        @DisplayName("KYC 응답 result가 null인 경우 BUSINESS_NOT_FOUND 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_KYC_result_null시_BUSINESS_NOT_FOUND_예외_발생() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", null);

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> authService.verifyBusiness(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.BUSINESS_NOT_FOUND));
        }

        @Test
        @DisplayName("신규 사업자 KYC 성공 시 BusinessVerificationResponse를 반환하고 세션에 processId를 저장한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_신규_사업자_KYC_성공시_응답_반환_및_세션_저장() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            ExternalKycResponse validKyc = new ExternalKycResponse(
                    "1234567890", "홍길동", "음식점업", "한식", "테스트상호", "서울시", "2020-01-01", true
            );
            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", validKyc);

            RegistrationProcess savedProcess = createRegistrationProcess(
                    10L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now()
            );

            // 첫 번째 execute() → 체크 + 기존 프로세스 조회
            // 두 번째 execute() → 신규 프로세스 저장
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);
            given(registrationProcessRepository.save(any(RegistrationProcess.class)))
                    .willReturn(savedProcess);

            // when
            BusinessVerificationResponse response = authService.verifyBusiness(request, session);

            // then
            assertThat(response).isNotNull();
            assertThat(response.businessNumber()).isEqualTo("1234567890");
            assertThat(response.representativeName()).isEqualTo("홍길동");
            verify(session).setAttribute("registrationProcessId", 10L);
        }

        @Test
        @DisplayName("유효한 기존 프로세스가 있으면 외부 API 호출 없이 캐시된 결과를 반환한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_유효한_기존_프로세스_있으면_캐시된_결과_반환() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess existingProcess = createRegistrationProcess(
                    5L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now().minusMinutes(10)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.of(existingProcess));

            // when
            BusinessVerificationResponse response = authService.verifyBusiness(request, session);

            // then
            assertThat(response).isNotNull();
            // 외부 API 호출 없음
            verify(externalMockClient, never()).callKycVerify(anyString());
            verify(session).setAttribute("registrationProcessId", 5L);
        }

        @Test
        @DisplayName("기존 프로세스가 만료된 경우 외부 API를 다시 호출하고 기존 프로세스를 업데이트한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_기존_프로세스_만료시_외부_API_재호출_후_업데이트() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess expiredProcess = createRegistrationProcess(
                    5L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now().minusMinutes(31)
            );

            ExternalKycResponse validKyc = new ExternalKycResponse(
                    "1234567890", "홍길동", "음식점업", "한식", "테스트상호", "서울시", "2020-01-01", true
            );
            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", validKyc);

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.of(expiredProcess));
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);
            given(registrationProcessRepository.save(any(RegistrationProcess.class)))
                    .willReturn(expiredProcess);

            // when
            BusinessVerificationResponse response = authService.verifyBusiness(request, session);

            // then
            assertThat(response).isNotNull();
            assertThat(response.businessNumber()).isEqualTo("1234567890");
            verify(externalMockClient).callKycVerify("1234567890");
            verify(session).setAttribute("registrationProcessId", 5L);
        }

        @Test
        @DisplayName("기존 프로세스가 있지만 step이 KYC_VERIFIED가 아닌 경우 삭제 후 새로 생성한다")
        @SuppressWarnings("unchecked")
        void verifyBusiness_기존_프로세스_step_다르면_삭제_후_새로_생성() {
            // given
            BusinessVerificationRequest request = createBusinessVerificationRequest("1234567890");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess oldProcess = createRegistrationProcess(
                    5L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(10)
            );

            ExternalKycResponse validKyc = new ExternalKycResponse(
                    "1234567890", "홍길동", "음식점업", "한식", "테스트상호", "서울시", "2020-01-01", true
            );
            ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", validKyc);

            RegistrationProcess newProcess = createRegistrationProcess(
                    11L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now()
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(businessProfileRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.empty());
            given(registrationProcessRepository.findByBusinessNumber("1234567890"))
                    .willReturn(Optional.of(oldProcess));
            given(externalMockClient.callKycVerify("1234567890")).willReturn(mockResponse);
            given(registrationProcessRepository.save(any(RegistrationProcess.class)))
                    .willReturn(newProcess);

            // when
            BusinessVerificationResponse response = authService.verifyBusiness(request, session);

            // then
            assertThat(response).isNotNull();
            verify(registrationProcessRepository).delete(oldProcess);
            verify(registrationProcessRepository).flush();
            verify(session).setAttribute("registrationProcessId", 11L);
        }
    }

    // ===== completeSignup 테스트 =====

    @Nested
    @DisplayName("completeSignup")
    class CompleteSignupTest {

        @Test
        @DisplayName("세션에 processId가 없으면 STEP_NOT_COMPLETED 예외가 발생한다")
        void completeSignup_세션_processId_없으면_STEP_NOT_COMPLETED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.STEP_NOT_COMPLETED));
        }

        @Test
        @DisplayName("processId가 DB에 없으면 REGISTRATION_EXPIRED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_processId_DB에_없으면_REGISTRATION_EXPIRED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.REGISTRATION_EXPIRED));
        }

        @Test
        @DisplayName("프로세스가 만료된 경우 REGISTRATION_EXPIRED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_프로세스_만료된_경우_REGISTRATION_EXPIRED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess expiredProcess = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusHours(1)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(expiredProcess));

            org.mockito.Mockito.doAnswer(inv -> {
                java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = inv.getArgument(0);
                action.accept(null);
                return null;
            }).when(transactionTemplate).executeWithoutResult(any());

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.REGISTRATION_EXPIRED));
        }

        @Test
        @DisplayName("PIN 인증이 완료되지 않은 경우 STEP_NOT_COMPLETED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_PIN_인증_미완료시_STEP_NOT_COMPLETED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess kycOnlyProcess = createRegistrationProcess(
                    1L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(kycOnlyProcess));

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.STEP_NOT_COMPLETED));
        }

        @Test
        @DisplayName("약관 ID 중 존재하지 않는 것이 있으면 TERM_NOT_FOUND 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_존재하지_않는_약관ID_TERM_NOT_FOUND_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            // termIds 2개 요청했는데 1개만 반환 → size 불일치
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList()))
                    .willReturn(List.of(createTerm(1L, TermType.PERSONAL_INFO, true)));

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(com.sofit.user.domain.terms.exception.TermErrorCode.TERM_NOT_FOUND));
        }

        @Test
        @DisplayName("약관 타입이 PERSONAL_INFO가 아닌 경우 TERM_TYPE_MISMATCH 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_약관_타입_불일치시_TERM_TYPE_MISMATCH_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            // 하나는 PERSONAL_INFO, 하나는 다른 타입 (LOAN_APPLICATION 등)
            List<Term> terms = List.of(
                    createTerm(1L, TermType.PERSONAL_INFO, true),
                    createTerm(2L, TermType.LOAN_APPLICATION, false)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList())).willReturn(terms);

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(com.sofit.user.domain.terms.exception.TermErrorCode.TERM_TYPE_MISMATCH));
        }

        @Test
        @DisplayName("필수 약관에 동의하지 않은 경우 REQUIRED_TERM_NOT_CONSENTED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_필수_약관_미동의시_REQUIRED_TERM_NOT_CONSENTED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequestWithConsent(false);
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            List<Term> terms = List.of(
                    createTerm(1L, TermType.PERSONAL_INFO, true),
                    createTerm(2L, TermType.PERSONAL_INFO, false)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList())).willReturn(terms);

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(com.sofit.user.domain.terms.exception.TermErrorCode.REQUIRED_TERM_NOT_CONSENTED));
        }

        @Test
        @DisplayName("이미 사용 중인 loginId로 가입 시 LOGIN_ID_DUPLICATED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void completeSignup_중복_loginId_LOGIN_ID_DUPLICATED_예외_발생() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );
            List<Term> terms = List.of(
                    createTerm(1L, TermType.PERSONAL_INFO, true),
                    createTerm(2L, TermType.PERSONAL_INFO, false)
            );

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList())).willReturn(terms);
            given(userRepository.existsByLoginId("newuser1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.completeSignup(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.LOGIN_ID_DUPLICATED));
        }

        @Test
        @DisplayName("정상 가입 완료 시 SignupCompleteResponse를 반환하고 세션에서 processId를 제거한다")
        @SuppressWarnings("unchecked")
        void completeSignup_정상_가입_완료시_SignupCompleteResponse_반환() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );
            List<Term> terms = List.of(
                    createTerm(1L, TermType.PERSONAL_INFO, true),
                    createTerm(2L, TermType.PERSONAL_INFO, false)
            );
            User savedUser = createActiveUser(99L, "newuser1", "홍길동");

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList())).willReturn(terms);
            given(userRepository.existsByLoginId("newuser1")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(businessProfileRepository.save(any())).willReturn(null);
            given(consentHistoryRepository.saveAll(anyList())).willReturn(List.of());

            // when
            SignupCompleteResponse response = authService.completeSignup(request, session);

            // then
            assertThat(response).isNotNull();
            assertThat(response.loginId()).isEqualTo("newuser1");
            assertThat(response.name()).isEqualTo("홍길동");
            verify(session).removeAttribute("registrationProcessId");
        }

        @Test
        @DisplayName("openDate가 null인 경우에도 정상 가입이 완료된다")
        @SuppressWarnings("unchecked")
        void completeSignup_openDate_null인_경우_정상_가입_완료() {
            // given
            SignupCompleteRequest request = createSignupCompleteRequest();
            HttpSession session = mock(HttpSession.class);
            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            RegistrationProcess process = createRegistrationProcessWithNullOpenDate(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );
            List<Term> terms = List.of(
                    createTerm(1L, TermType.PERSONAL_INFO, true),
                    createTerm(2L, TermType.PERSONAL_INFO, false)
            );
            User savedUser = createActiveUser(99L, "newuser1", "홍길동");

            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> cb = inv.getArgument(0);
                        return cb.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L)).willReturn(Optional.of(process));
            given(termRepository.findAllByTermIdInAndIsActiveTrue(anyList())).willReturn(terms);
            given(userRepository.existsByLoginId("newuser1")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(businessProfileRepository.save(any())).willReturn(null);
            given(consentHistoryRepository.saveAll(anyList())).willReturn(List.of());

            // when
            SignupCompleteResponse response = authService.completeSignup(request, session);

            // then
            assertThat(response).isNotNull();
            assertThat(response.loginId()).isEqualTo("newuser1");
            verify(session).removeAttribute("registrationProcessId");
        }
    }

    @Nested
    @DisplayName("verifyFinancialCertificate")
    class VerifyFinancialCertificateTest {

        @Test
        @DisplayName("세션에 processId가 없는 경우 FinancialCertService에만 위임하고 정상 완료된다")
        void verifyFinancialCertificate_세션_processId_없는_경우_위임_후_반환() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            given(session.getAttribute("registrationProcessId")).willReturn(null);

            // when
            authService.verifyFinancialCertificate(request, session);

            // then
            verify(financialCertService).verify(request);
            // processId 없으면 registrationProcessRepository 조회 안 함
            verify(registrationProcessRepository, never()).findById(any());
        }

        @Test
        @DisplayName("세션에 processId가 있고 만료된 경우 REGISTRATION_EXPIRED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyFinancialCertificate_세션_processId_있고_만료된_경우_REGISTRATION_EXPIRED_예외_발생() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess expiredProcess = createRegistrationProcess(
                    1L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now().minusHours(1)
            );

            given(session.getAttribute("registrationProcessId")).willReturn(1L);

            // TransactionTemplate.execute() 스텁 — processId 조회 반환
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(expiredProcess));

            // TransactionTemplate.executeWithoutResult() 스텁 — expire 저장
            org.mockito.Mockito.doAnswer(inv -> {
                Consumer<org.springframework.transaction.TransactionStatus> action = inv.getArgument(0);
                action.accept(null);
                return null;
            }).when(transactionTemplate).executeWithoutResult(any());

            // when & then
            assertThatThrownBy(() -> authService.verifyFinancialCertificate(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.REGISTRATION_EXPIRED));
        }

        @Test
        @DisplayName("이미 PIN 인증이 완료된 경우 STEP_ALREADY_COMPLETED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyFinancialCertificate_이미_PIN_인증_완료된_경우_STEP_ALREADY_COMPLETED_예외_발생() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess completedProcess = createRegistrationProcess(
                    1L, RegistrationStep.PIN_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            given(session.getAttribute("registrationProcessId")).willReturn(1L);
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(completedProcess));

            // when & then
            assertThatThrownBy(() -> authService.verifyFinancialCertificate(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.STEP_ALREADY_COMPLETED));
        }

        @Test
        @DisplayName("세션에 processId가 있지만 DB에 레코드가 없는 경우 STEP_NOT_COMPLETED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyFinancialCertificate_processId_있지만_DB에_없으면_STEP_NOT_COMPLETED_예외_발생() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            given(session.getAttribute("registrationProcessId")).willReturn(99L);
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.verifyFinancialCertificate(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.STEP_NOT_COMPLETED));
        }

        @Test
        @DisplayName("정상적으로 Step2를 완료하면 프로세스 상태가 변경된다")
        @SuppressWarnings("unchecked")
        void verifyFinancialCertificate_정상_Step2_완료시_프로세스_상태_변경() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            RegistrationProcess process = createRegistrationProcess(
                    1L, RegistrationStep.KYC_VERIFIED, LocalDateTime.now().minusMinutes(5)
            );

            given(session.getAttribute("registrationProcessId")).willReturn(1L);
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(process));
            org.mockito.Mockito.doAnswer(inv -> {
                Consumer<org.springframework.transaction.TransactionStatus> action = inv.getArgument(0);
                action.accept(null);
                return null;
            }).when(transactionTemplate).executeWithoutResult(any());

            // when
            authService.verifyFinancialCertificate(request, session);

            // then
            verify(financialCertService).verify(request);
            verify(registrationProcessRepository).save(process);
        }

        @Test
        @DisplayName("step이 EXPIRED인 경우 STEP_NOT_COMPLETED 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void verifyFinancialCertificate_step이_EXPIRED인_경우_STEP_NOT_COMPLETED_예외_발생() {
            // given
            var request = createFinancialCertRequest("01012345678", "123456");
            HttpSession session = mock(HttpSession.class);

            // step이 EXPIRED — PIN_VERIFIED도 KYC_VERIFIED도 아님
            RegistrationProcess expiredStepProcess = createRegistrationProcess(
                    1L, RegistrationStep.EXPIRED, LocalDateTime.now().minusMinutes(5)
            );

            given(session.getAttribute("registrationProcessId")).willReturn(1L);
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            given(registrationProcessRepository.findById(1L))
                    .willReturn(Optional.of(expiredStepProcess));

            // when & then
            assertThatThrownBy(() -> authService.verifyFinancialCertificate(request, session))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.STEP_NOT_COMPLETED));
        }
    }

    // ===== Helper Methods =====

    private User createActiveUser(Long userId, String loginId, String name) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private User createInactiveUser(Long userId, String loginId, String name) {
        User user = createActiveUser(userId, loginId, name);
        user.inactivate();
        return user;
    }

    private LoginRequest createLoginRequest(String loginId, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest createFinancialCertRequest(
            String phoneNumber, String pin) {
        var request = new com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest();
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        ReflectionTestUtils.setField(request, "pin", pin);
        return request;
    }

    private BusinessVerificationRequest createBusinessVerificationRequest(String businessNumber) {
        BusinessVerificationRequest request = new BusinessVerificationRequest();
        ReflectionTestUtils.setField(request, "businessNumber", businessNumber);
        return request;
    }

    private SignupCompleteRequest createSignupCompleteRequest() {
        SignupCompleteRequest request = new SignupCompleteRequest();
        ReflectionTestUtils.setField(request, "loginId", "newuser1");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "residentNumber", "9001011");
        ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");

        SignupCompleteRequest.ConsentItem item1 = new SignupCompleteRequest.ConsentItem();
        ReflectionTestUtils.setField(item1, "termId", 1L);
        ReflectionTestUtils.setField(item1, "isConsented", true);

        SignupCompleteRequest.ConsentItem item2 = new SignupCompleteRequest.ConsentItem();
        ReflectionTestUtils.setField(item2, "termId", 2L);
        ReflectionTestUtils.setField(item2, "isConsented", true);

        ReflectionTestUtils.setField(request, "consents", List.of(item1, item2));
        return request;
    }

    private SignupCompleteRequest createSignupCompleteRequestWithConsent(boolean firstTermConsented) {
        SignupCompleteRequest request = new SignupCompleteRequest();
        ReflectionTestUtils.setField(request, "loginId", "newuser1");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "residentNumber", "9001011");
        ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");

        SignupCompleteRequest.ConsentItem item1 = new SignupCompleteRequest.ConsentItem();
        ReflectionTestUtils.setField(item1, "termId", 1L);
        ReflectionTestUtils.setField(item1, "isConsented", firstTermConsented);

        SignupCompleteRequest.ConsentItem item2 = new SignupCompleteRequest.ConsentItem();
        ReflectionTestUtils.setField(item2, "termId", 2L);
        ReflectionTestUtils.setField(item2, "isConsented", true);

        ReflectionTestUtils.setField(request, "consents", List.of(item1, item2));
        return request;
    }

    private Term createTerm(Long termId, TermType termType, boolean isRequired) {
        try {
            var constructor = Term.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Term term = constructor.newInstance();
            ReflectionTestUtils.setField(term, "termId", termId);
            ReflectionTestUtils.setField(term, "termType", termType);
            ReflectionTestUtils.setField(term, "isRequired", isRequired);
            ReflectionTestUtils.setField(term, "isActive", true);
            return term;
        } catch (Exception e) {
            throw new RuntimeException("Term 생성 실패", e);
        }
    }

    /**
     * RegistrationProcess 테스트 인스턴스를 생성한다.
     * updatedAt은 BaseEntity에 있으므로 리플렉션으로 설정한다.
     */
    private RegistrationProcess createRegistrationProcess(Long id, RegistrationStep step,
                                                           LocalDateTime updatedAt) {
        RegistrationProcess process = RegistrationProcess.createForStep1(
                "1234567890", "테스트상호", "홍길동", LocalDate.parse("2020-01-01"), "한식", "음식점업", "서울시"
        );
        ReflectionTestUtils.setField(process, "registrationProcessId", id);
        ReflectionTestUtils.setField(process, "step", step);
        // BaseEntity의 updatedAt 설정
        setBaseEntityField(process, "updatedAt", updatedAt);
        return process;
    }

    /**
     * openDate가 null인 RegistrationProcess 인스턴스를 생성한다.
     * completeSignup에서 openDate null 삼항 분기를 테스트한다.
     */
    private RegistrationProcess createRegistrationProcessWithNullOpenDate(Long id, RegistrationStep step,
                                                                           LocalDateTime updatedAt) {
        RegistrationProcess process = RegistrationProcess.createForStep1(
                "1234567890", "테스트상호", "홍길동", null, "한식", "음식점업", "서울시"
        );
        ReflectionTestUtils.setField(process, "registrationProcessId", id);
        ReflectionTestUtils.setField(process, "step", step);
        setBaseEntityField(process, "updatedAt", updatedAt);
        return process;
    }

    private void setBaseEntityField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("BaseEntity 필드 설정 실패: " + fieldName, e);
        }
    }
}
