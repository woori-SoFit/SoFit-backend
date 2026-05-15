package com.sofit.user.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * External Mock 서버의 공통 응답 포맷을 역직렬화하기 위한 DTO.
 * 우리 서버의 ApiResponse와 구조는 같지만, 역직렬화 전용으로 분리한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalMockApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess,
        String code,
        String message,
        T result
) {
}
