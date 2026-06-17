package com.sofit.user.domain.auth.client;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertLookupRequest;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertRequest;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalMockClient 단위 테스트")
class ExternalMockClientTest {

    @InjectMocks
    private ExternalMockClient externalMockClient;

    @Mock
    private RestClient externalMockRestClient;

    @Nested
    @DisplayName("callKycVerify")
    class CallKycVerifyTest {

        @Test
        @DisplayName("정상 호출 시 응답을 반환한다")
        void shouldReturnResponseOnSuccess() {
            // given
            ExternalMockApiResponse<ExternalKycResponse> expected = new ExternalMockApiResponse<>(
                    true, "SUCCESS", "성공", null);

            RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            given(externalMockRestClient.post()).willReturn(bodyUriSpec);
            given(bodyUriSpec.uri("/ext/kyc/verify")).willReturn(bodySpec);
            given(bodySpec.body(any(Object.class))).willReturn(bodySpec);
            given(bodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(expected);

            // when
            ExternalMockApiResponse<ExternalKycResponse> result = externalMockClient.callKycVerify("1234567890");

            // then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("RestClient 예외 시 EXTERNAL_SERVER_ERROR를 던진다")
        void shouldThrowOnRestClientException() {
            // given
            RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            given(externalMockRestClient.post()).willReturn(bodyUriSpec);
            given(bodyUriSpec.uri("/ext/kyc/verify")).willThrow(new RestClientException("Connection refused"));

            // when & then
            assertThatThrownBy(() -> externalMockClient.callKycVerify("1234567890"))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> {
                        BaseException be = (BaseException) e;
                        assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.EXTERNAL_SERVER_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("callFinancialCertIdentityVerify")
    class CallFinancialCertIdentityVerifyTest {

        @Test
        @DisplayName("RestClient 예외 시 EXTERNAL_SERVER_ERROR를 던진다")
        void shouldThrowOnRestClientException() {
            // given
            ExternalFinancialCertRequest request = new ExternalFinancialCertRequest(
                    "01012345678", "홍길동", "9001011", "123456");

            RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            given(externalMockRestClient.post()).willReturn(bodyUriSpec);
            given(bodyUriSpec.uri("/ext/financial-certs/identity-verify"))
                    .willThrow(new RestClientException("Timeout"));

            // when & then
            assertThatThrownBy(() -> externalMockClient.callFinancialCertIdentityVerify(request))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> {
                        BaseException be = (BaseException) e;
                        assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.EXTERNAL_SERVER_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("callFinancialCertLookup")
    class CallFinancialCertLookupTest {

        @Test
        @DisplayName("RestClient 예외 시 EXTERNAL_SERVER_ERROR를 던진다")
        void shouldThrowOnRestClientException() {
            // given
            ExternalFinancialCertLookupRequest request = new ExternalFinancialCertLookupRequest(
                    "홍길동", "9001011", "01012345678");

            RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            given(externalMockRestClient.post()).willReturn(bodyUriSpec);
            given(bodyUriSpec.uri("/ext/financial-certs/lookup"))
                    .willThrow(new RestClientException("Timeout"));

            // when & then
            assertThatThrownBy(() -> externalMockClient.callFinancialCertLookup(request))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> {
                        BaseException be = (BaseException) e;
                        assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.EXTERNAL_SERVER_ERROR);
                    });
        }
    }
}
