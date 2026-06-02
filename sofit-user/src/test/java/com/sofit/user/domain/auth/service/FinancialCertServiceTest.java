package com.sofit.user.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;

@ExtendWith(MockitoExtension.class)
class FinancialCertServiceTest {

    @InjectMocks
    private FinancialCertService financialCertService;

    @Mock
    private ExternalMockClient externalMockClient;

    private static final String PHONE_NUMBER = "01012345678";
    private static final String VALID_PIN = "123456";

    // ===== verify 테스트 =====

    @Test
    @DisplayName("정상 PIN 인증 시 FinancialCertVerifyResponse를 반환한다")
    void verify_정상_PIN_인증시_FinancialCertVerifyResponse_반환() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN);
        ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                PHONE_NUMBER, "CERT-001", "홍길동", "VALID", "2024-01-01", "2026-01-01"
        );
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                new ExternalMockApiResponse<>(true, "AUTH2002", "성공", certResult);

        given(externalMockClient.callFinancialCertVerify(PHONE_NUMBER, VALID_PIN))
                .willReturn(mockResponse);

        // when
        FinancialCertVerifyResponse response = financialCertService.verify(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.certNumber()).isEqualTo("CERT-001");
        assertThat(response.holderName()).isEqualTo("홍길동");
        assertThat(response.phoneNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(response.status()).isEqualTo("VALID");
        assertThat(response.verifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("외부 서버가 AUTH4001 코드 반환 시 PIN_MISMATCH 예외가 발생한다")
    void verify_외부서버_AUTH4001_반환시_PIN_MISMATCH_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, "000000");
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                new ExternalMockApiResponse<>(false, "AUTH4001", "PIN 번호가 올바르지 않습니다.", null);

        given(externalMockClient.callFinancialCertVerify(PHONE_NUMBER, "000000"))
                .willReturn(mockResponse);

        // when & then
        assertThatThrownBy(() -> financialCertService.verify(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(AuthErrorCode.PIN_MISMATCH);
                });
    }

    @Test
    @DisplayName("외부 서버가 다른 실패 코드 반환 시 CERT_NOT_FOUND 예외가 발생한다")
    void verify_외부서버_기타_실패_반환시_CERT_NOT_FOUND_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN);
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                new ExternalMockApiResponse<>(false, "AUTH4042", "등록된 금융인증서를 찾을 수 없습니다.", null);

        given(externalMockClient.callFinancialCertVerify(PHONE_NUMBER, VALID_PIN))
                .willReturn(mockResponse);

        // when & then
        assertThatThrownBy(() -> financialCertService.verify(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(AuthErrorCode.CERT_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("인증서 상태가 VALID가 아닌 경우 CERT_VERIFICATION_FAILED 예외가 발생한다")
    void verify_인증서_상태_VALID_아닌_경우_CERT_VERIFICATION_FAILED_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN);
        ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                PHONE_NUMBER, "CERT-001", "홍길동", "EXPIRED", "2022-01-01", "2024-01-01"
        );
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                new ExternalMockApiResponse<>(true, "AUTH2002", "성공", certResult);

        given(externalMockClient.callFinancialCertVerify(PHONE_NUMBER, VALID_PIN))
                .willReturn(mockResponse);

        // when & then
        assertThatThrownBy(() -> financialCertService.verify(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(AuthErrorCode.CERT_VERIFICATION_FAILED);
                });
    }

    @Test
    @DisplayName("인증서 상태가 REVOKED인 경우 CERT_VERIFICATION_FAILED 예외가 발생한다")
    void verify_인증서_상태_REVOKED인_경우_CERT_VERIFICATION_FAILED_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN);
        ExternalFinancialCertResponse certResult = new ExternalFinancialCertResponse(
                PHONE_NUMBER, "CERT-001", "홍길동", "REVOKED", "2023-01-01", "2025-01-01"
        );
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                new ExternalMockApiResponse<>(true, "AUTH2002", "성공", certResult);

        given(externalMockClient.callFinancialCertVerify(PHONE_NUMBER, VALID_PIN))
                .willReturn(mockResponse);

        // when & then
        assertThatThrownBy(() -> financialCertService.verify(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(AuthErrorCode.CERT_VERIFICATION_FAILED);
                });
    }

    // ===== Helper Methods =====

    private FinancialCertVerifyRequest createRequest(String phoneNumber, String pin) {
        FinancialCertVerifyRequest request = new FinancialCertVerifyRequest();
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        ReflectionTestUtils.setField(request, "pin", pin);
        return request;
    }
}
