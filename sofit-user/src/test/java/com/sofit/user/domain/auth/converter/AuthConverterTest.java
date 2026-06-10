package com.sofit.user.domain.auth.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertLookupResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;

class AuthConverterTest {

    @Nested
    @DisplayName("toBusinessVerificationResponse (ExternalKycResponse)")
    class ToBusinessVerificationResponseFromKycTest {

        @Test
        @DisplayName("ExternalKycResponse를 BusinessVerificationResponse로 변환한다")
        void toBusinessVerificationResponse_정상_변환() {
            // given
            ExternalKycResponse kycResult = new ExternalKycResponse(
                    "1234567890", "홍길동", "한식", "음식점업",
                    "맛있는식당", "서울시", "2020-03-15", true
            );

            // when
            BusinessVerificationResponse response = AuthConverter.toBusinessVerificationResponse(kycResult);

            // then
            assertThat(response.businessNumber()).isEqualTo("1234567890");
            assertThat(response.representativeName()).isEqualTo("홍길동");
            assertThat(response.businessName()).isEqualTo("맛있는식당");
            assertThat(response.businessType()).isEqualTo("음식점업");
            assertThat(response.openDate()).isEqualTo(LocalDate.of(2020, 3, 15));
            assertThat(response.verifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("openDate가 null인 경우 null로 변환한다")
        void toBusinessVerificationResponse_openDate_null_변환() {
            // given
            ExternalKycResponse kycResult = new ExternalKycResponse(
                    "9876543210", "김대표", "소매업", "편의점",
                    "김편의점", "부산시", null, true
            );

            // when
            BusinessVerificationResponse response = AuthConverter.toBusinessVerificationResponse(kycResult);

            // then
            assertThat(response.openDate()).isNull();
            assertThat(response.businessNumber()).isEqualTo("9876543210");
        }

        @Test
        @DisplayName("openDate가 빈 문자열인 경우 null로 변환한다")
        void toBusinessVerificationResponse_openDate_빈문자열_변환() {
            // given
            ExternalKycResponse kycResult = new ExternalKycResponse(
                    "1111111111", "박대표", "IT", "소프트웨어",
                    "박회사", "대전시", "", true
            );

            // when
            BusinessVerificationResponse response = AuthConverter.toBusinessVerificationResponse(kycResult);

            // then
            assertThat(response.openDate()).isNull();
        }
    }

    @Nested
    @DisplayName("toBusinessVerificationResponse (RegistrationProcess)")
    class ToBusinessVerificationResponseFromProcessTest {

        @Test
        @DisplayName("RegistrationProcess를 BusinessVerificationResponse로 변환한다")
        void toBusinessVerificationResponse_프로세스_정상_변환() {
            // given
            RegistrationProcess process = RegistrationProcess.createForStep1(
                    "1234567890", "맛있는식당", "홍길동",
                    LocalDate.of(2020, 3, 15), "한식", "음식점업", "서울시"
            );
            ReflectionTestUtils.setField(process, "createdAt", LocalDateTime.of(2024, 6, 1, 10, 30));

            // when
            BusinessVerificationResponse response = AuthConverter.toBusinessVerificationResponse(process);

            // then
            assertThat(response.businessNumber()).isEqualTo("1234567890");
            assertThat(response.representativeName()).isEqualTo("홍길동");
            assertThat(response.businessName()).isEqualTo("맛있는식당");
            assertThat(response.businessType()).isEqualTo("한식");
            assertThat(response.openDate()).isEqualTo(LocalDate.of(2020, 3, 15));
            assertThat(response.verifiedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 30));
        }
    }

    @Nested
    @DisplayName("toLoginResponse")
    class ToLoginResponseTest {

        @Test
        @DisplayName("User 엔티티를 LoginResponse로 변환한다")
        void toLoginResponse_정상_변환() {
            // given
            User user = User.createUser("testuser", "hashedPw", "홍길동", "01012345678", "9001011");
            ReflectionTestUtils.setField(user, "userId", 5L);

            // when
            LoginResponse response = AuthConverter.toLoginResponse(user);

            // then
            assertThat(response.userId()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.role()).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("toSignupCompleteResponse")
    class ToSignupCompleteResponseTest {

        @Test
        @DisplayName("User 엔티티를 SignupCompleteResponse로 변환한다")
        void toSignupCompleteResponse_정상_변환() {
            // given
            User user = User.createUser("newuser1", "hashedPw", "김철수", "01099998888", "9501012");
            ReflectionTestUtils.setField(user, "userId", 10L);

            // when
            SignupCompleteResponse response = AuthConverter.toSignupCompleteResponse(user);

            // then
            assertThat(response.userId()).isEqualTo(10L);
            assertThat(response.loginId()).isEqualTo("newuser1");
            assertThat(response.name()).isEqualTo("김철수");
            assertThat(response.role()).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("toFinancialCertLookupResponse")
    class ToFinancialCertLookupResponseTest {

        @Test
        @DisplayName("ExternalFinancialCertResponse를 FinancialCertLookupResponse로 변환한다")
        void toFinancialCertLookupResponse_정상_변환() {
            // given
            ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                    "01012345678", "CERT-001", "홍길동", "VALID",
                    "2024-01-01T00:00:00", "2026-01-01T00:00:00"
            );

            // when
            FinancialCertLookupResponse response = AuthConverter.toFinancialCertLookupResponse(certResult);

            // then
            assertThat(response.phoneNumber()).isEqualTo("01012345678");
            assertThat(response.certNumber()).isEqualTo("CERT-001");
            assertThat(response.holderName()).isEqualTo("홍길동");
            assertThat(response.status()).isEqualTo("VALID");
            assertThat(response.issuedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
            assertThat(response.expiresAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        }

        @Test
        @DisplayName("issuedAt과 expiresAt이 null인 경우 null로 변환한다")
        void toFinancialCertLookupResponse_날짜_null_변환() {
            // given
            ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                    "01099998888", "CERT-002", "김철수", "EXPIRED",
                    null, null
            );

            // when
            FinancialCertLookupResponse response = AuthConverter.toFinancialCertLookupResponse(certResult);

            // then
            assertThat(response.issuedAt()).isNull();
            assertThat(response.expiresAt()).isNull();
            assertThat(response.status()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("issuedAt과 expiresAt이 빈 문자열인 경우 null로 변환한다")
        void toFinancialCertLookupResponse_날짜_빈문자열_변환() {
            // given
            ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                    "01055556666", "CERT-003", "박대표", "VALID",
                    "", ""
            );

            // when
            FinancialCertLookupResponse response = AuthConverter.toFinancialCertLookupResponse(certResult);

            // then
            assertThat(response.issuedAt()).isNull();
            assertThat(response.expiresAt()).isNull();
        }
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
