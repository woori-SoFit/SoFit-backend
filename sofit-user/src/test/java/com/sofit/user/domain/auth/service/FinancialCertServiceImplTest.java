package com.sofit.user.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sofit.user.domain.auth.client.ExternalMockClient;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;

@ExtendWith(MockitoExtension.class)
class FinancialCertServiceImplTest {

    @InjectMocks
    private FinancialCertServiceImpl financialCertService;

    @Mock
    private ExternalMockClient externalMockClient;

    private static final String PHONE_NUMBER = "01012345678";
    private static final String VALID_PIN = "123456";
    private static final String HOLDER_NAME = "홍길동";
    private static final String RESIDENT_NUMBER = "0110144";

    // ===== verify 테스트 =====

    @Test
    @DisplayName("정상 PIN 인증 시 예외 없이 정상 처리된다")
    void verify_정상_PIN_인증시_예외없이_정상_처리() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN, HOLDER_NAME, RESIDENT_NUMBER);
        ExternalMockApiResponse<Void> mockResponse =
                new ExternalMockApiResponse<>(true, "AUTH2002", "성공", null);

        given(externalMockClient.callFinancialCertIdentityVerify(any(ExternalFinancialCertRequest.class)))
                .willReturn(mockResponse);

        // when & then - 예외 없이 정상 완료
        financialCertService.verify(request);
    }

    @Test
    @DisplayName("외부 서버가 AUTH4001 코드 반환 시 PIN_MISMATCH 예외가 발생한다")
    void verify_외부서버_AUTH4001_반환시_PIN_MISMATCH_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, "000000", HOLDER_NAME, RESIDENT_NUMBER);
        ExternalMockApiResponse<Void> mockResponse =
                new ExternalMockApiResponse<>(false, "AUTH4001", "PIN 번호가 올바르지 않습니다.", null);

        given(externalMockClient.callFinancialCertIdentityVerify(any(ExternalFinancialCertRequest.class)))
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
    @DisplayName("외부 서버가 다른 실패 코드 반환 시 CERT_VERIFICATION_FAILED 예외가 발생한다")
    void verify_외부서버_기타_실패_반환시_CERT_VERIFICATION_FAILED_예외_발생() {
        // given
        FinancialCertVerifyRequest request = createRequest(PHONE_NUMBER, VALID_PIN, HOLDER_NAME, RESIDENT_NUMBER);
        ExternalMockApiResponse<Void> mockResponse =
                new ExternalMockApiResponse<>(false, "AUTH4042", "등록된 금융인증서를 찾을 수 없습니다.", null);

        given(externalMockClient.callFinancialCertIdentityVerify(any(ExternalFinancialCertRequest.class)))
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

    private FinancialCertVerifyRequest createRequest(String phoneNumber, String pin, String holderName, String residentNumber) {
        FinancialCertVerifyRequest request = new FinancialCertVerifyRequest();
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        ReflectionTestUtils.setField(request, "pin", pin);
        ReflectionTestUtils.setField(request, "holderName", holderName);
        ReflectionTestUtils.setField(request, "residentNumber", residentNumber);
        return request;
    }
}
