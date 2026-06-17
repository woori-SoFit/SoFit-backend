package com.sofit.user.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.client.ExternalMockClient;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.request.FinancialCertLookupRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.FinancialCertLookupResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialCertServiceImpl 단위 테스트")
class FinancialCertServiceImplTest {

    @InjectMocks
    private FinancialCertServiceImpl financialCertService;

    @Mock
    private ExternalMockClient externalMockClient;

    @Nested
    @DisplayName("lookup")
    class LookupTest {

        @Test
        @DisplayName("정상 조회 시 FinancialCertLookupResponse를 반환한다")
        void lookup_정상_조회시_응답_반환() {
            // given
            FinancialCertLookupRequest request = createLookupRequest("홍길동", "9001011", "01012345678");

            ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                    "01012345678", "CERT-001", "홍길동", "VALID",
                    "2025-01-01T00:00:00", "2026-01-01T00:00:00"
            );
            ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", certResult);

            given(externalMockClient.callFinancialCertLookup(any())).willReturn(mockResponse);

            // when
            FinancialCertLookupResponse response = financialCertService.lookup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.phoneNumber()).isEqualTo("01012345678");
            assertThat(response.certNumber()).isEqualTo("CERT-001");
            assertThat(response.holderName()).isEqualTo("홍길동");
            assertThat(response.status()).isEqualTo("VALID");
        }

        @Test
        @DisplayName("External Mock 서버가 실패 응답을 반환하면 CERT_NOT_FOUND 예외를 던진다")
        void lookup_실패_응답시_CERT_NOT_FOUND_예외() {
            // given
            FinancialCertLookupRequest request = createLookupRequest("홍길동", "9001011", "01012345678");

            ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                    new ExternalMockApiResponse<>(false, "AUTH4041", "인증서를 찾을 수 없습니다.", null);

            given(externalMockClient.callFinancialCertLookup(any())).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> financialCertService.lookup(request))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.CERT_NOT_FOUND));
        }

        @Test
        @DisplayName("issuedAt/expiresAt이 null인 경우에도 정상 반환한다")
        void lookup_날짜_null인_경우에도_정상_반환() {
            // given
            FinancialCertLookupRequest request = createLookupRequest("홍길동", "9001011", "01012345678");

            ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                    "01012345678", "CERT-002", "홍길동", "VALID", null, null
            );
            ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", certResult);

            given(externalMockClient.callFinancialCertLookup(any())).willReturn(mockResponse);

            // when
            FinancialCertLookupResponse response = financialCertService.lookup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.issuedAt()).isNull();
            assertThat(response.expiresAt()).isNull();
        }
    }

    @Nested
    @DisplayName("verify")
    class VerifyTest {

        @Test
        @DisplayName("정상 검증 시 예외 없이 완료된다")
        void verify_정상_검증시_예외_없이_완료() {
            // given
            FinancialCertVerifyRequest request = createVerifyRequest("01012345678", "123456");

            ExternalMockApiResponse<Void> mockResponse =
                    new ExternalMockApiResponse<>(true, "AUTH2001", "성공", null);

            given(externalMockClient.callFinancialCertIdentityVerify(any())).willReturn(mockResponse);

            // when & then (예외 없이 정상 종료)
            financialCertService.verify(request);
        }

        @Test
        @DisplayName("PIN이 불일치하면 PIN_MISMATCH 예외를 던진다")
        void verify_PIN_불일치시_PIN_MISMATCH_예외() {
            // given
            FinancialCertVerifyRequest request = createVerifyRequest("01012345678", "999999");

            ExternalMockApiResponse<Void> mockResponse =
                    new ExternalMockApiResponse<>(false, "AUTH4001", "PIN이 일치하지 않습니다.", null);

            given(externalMockClient.callFinancialCertIdentityVerify(any())).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> financialCertService.verify(request))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.PIN_MISMATCH));
        }

        @Test
        @DisplayName("PIN 외 기타 검증 실패 시 CERT_VERIFICATION_FAILED 예외를 던진다")
        void verify_기타_검증_실패시_CERT_VERIFICATION_FAILED_예외() {
            // given
            FinancialCertVerifyRequest request = createVerifyRequest("01012345678", "123456");

            ExternalMockApiResponse<Void> mockResponse =
                    new ExternalMockApiResponse<>(false, "AUTH4002", "인증서가 만료되었습니다.", null);

            given(externalMockClient.callFinancialCertIdentityVerify(any())).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> financialCertService.verify(request))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.CERT_VERIFICATION_FAILED));
        }
    }

    // ===== Helper Methods =====

    private FinancialCertLookupRequest createLookupRequest(String holderName, String residentNumber, String phoneNumber) {
        FinancialCertLookupRequest request = new FinancialCertLookupRequest();
        ReflectionTestUtils.setField(request, "holderName", holderName);
        ReflectionTestUtils.setField(request, "residentNumber", residentNumber);
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        return request;
    }

    private FinancialCertVerifyRequest createVerifyRequest(String phoneNumber, String pin) {
        FinancialCertVerifyRequest request = new FinancialCertVerifyRequest();
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        ReflectionTestUtils.setField(request, "pin", pin);
        return request;
    }
}
