package com.sofit.admin.domain.loan.client;

import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.notification.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPushClient 단위 테스트")
class NotificationPushClientTest {

    @InjectMocks
    private NotificationPushClient notificationPushClient;

    @Mock
    private RestClient notificationRestClient;

    @Test
    @DisplayName("pushNotification - 정상적으로 알림을 전송한다")
    void pushNotification_success() {
        // given
        NotificationPushRequest request = createRequest();

        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(notificationRestClient.post()).willReturn(bodyUriSpec);
        given(bodyUriSpec.uri("/api/notifications/internal/push")).willReturn(bodySpec);
        given(bodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(bodySpec);
        given(bodySpec.body(request)).willReturn(bodySpec);
        given(bodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.toBodilessEntity()).willReturn(null);

        // when & then
        assertThatCode(() -> notificationPushClient.pushNotification(request))
                .doesNotThrowAnyException();

        verify(notificationRestClient).post();
    }

    @Test
    @DisplayName("pushNotification - 전송 실패 시 예외를 전파하지 않는다")
    void pushNotification_failureShouldNotThrow() {
        // given
        NotificationPushRequest request = createRequest();

        given(notificationRestClient.post()).willThrow(new RestClientException("Connection refused"));

        // when & then — 예외가 전파되지 않아야 함
        assertThatCode(() -> notificationPushClient.pushNotification(request))
                .doesNotThrowAnyException();
    }

    private NotificationPushRequest createRequest() {
        return NotificationPushRequest.builder()
                .userId(1L)
                .notificationId(100L)
                .type(NotificationType.LOAN_DECIDED)
                .title("대출 심사 완료")
                .message("대출 심사가 완료되었습니다.")
                .referenceId(10L)
                .referenceLabel("대출 신청")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
    }
}
