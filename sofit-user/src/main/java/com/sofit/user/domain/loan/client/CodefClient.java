package com.sofit.user.domain.loan.client;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 코데프 데모 서버와 HTTP 통신을 담당하는 클라이언트
 * - OAuth 2.0 토큰 발급/캐싱/재발급
 * - 1원 이체 인증 요청
 */
@Slf4j
@Component
@EnableConfigurationProperties(CodefProperties.class)
public class CodefClient {

    private static final String TRANSFER_AUTH_PATH = "/v1/kr/bank/a/account/transfer-authentication";
    private static final String CODEF_SUCCESS_CODE = "CF-00000";
    private static final String TOKEN_CACHE_KEY = "codef:access-token";
    private static final long TOKEN_CACHE_TTL_DAYS = 6; // 일주일 유효, 6일 캐싱
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final CodefProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public CodefClient(CodefProperties properties, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) TIMEOUT.toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 코데프 1원 이체 인증 요청
     *
     * @param organization 은행 기관코드
     * @param account 계좌번호
     * @return 4자리 인증코드 (authCode)
     * @throws BaseException ACCOUNT5001 - 코데프 API 호출 실패
     */
    public String requestOneWonTransfer(String organization, String account) {
        String accessToken = getAccessToken();

        try {
            String responseBody = callTransferApi(accessToken, organization, account);
            return extractAuthCode(responseBody);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            // 401 토큰 만료 시 재발급 후 1회 재시도
            log.warn("코데프 API 호출 실패, 토큰 재발급 후 재시도: {}", e.getMessage());
            refreshAccessToken();
            try {
                String retryToken = getAccessToken();
                String responseBody = callTransferApi(retryToken, organization, account);
                return extractAuthCode(responseBody);
            } catch (Exception retryEx) {
                log.error("코데프 API 재시도 실패: {}", retryEx.getMessage());
                throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
            }
        }
    }

    /**
     * 코데프 1원 이체 API 호출
     */
    private String callTransferApi(String accessToken, String organization, String account) {
        Map<String, String> requestBody = Map.of(
                "organization", organization,
                "account", account,
                "inPrintType", "2",
                "inPrintContent", "SOFIT"
        );

        try {
            String body = objectMapper.writeValueAsString(requestBody);

            String response = restClient.post()
                    .uri(properties.getBaseUrl() + TRANSFER_AUTH_PATH)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("코데프 API 원본 응답 (앞 200자): {}", response != null && response.length() > 200 ? response.substring(0, 200) : response);

            // 코데프 API 응답은 URL 인코딩되어 옴 → 디코딩
            return URLDecoder.decode(response, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("코데프 API 호출 중 예외 발생: {}", e.getMessage());
            throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
        }
    }

    /**
     * 코데프 응답에서 authCode 추출
     * 코데프 API 응답은 URL 인코딩되어 오므로 디코딩 후 JSON 파싱
     * result.code == "CF-00000" 확인 후 data.authCode 반환
     */
    private String extractAuthCode(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.get("result");

            if (result == null || !CODEF_SUCCESS_CODE.equals(result.get("code").asText())) {
                String errorCode = result != null ? result.get("code").asText() : "UNKNOWN";
                String errorMessage = result != null ? result.get("message").asText() : "응답 파싱 실패";
                log.error("코데프 API 에러 응답 - code: {}, message: {}", errorCode, errorMessage);
                throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("authCode")) {
                log.error("코데프 API 응답에 authCode가 없습니다");
                throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
            }

            return data.get("authCode").asText();
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("코데프 API 응답 파싱 실패: {}", e.getMessage());
            throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
        }
    }

    /**
     * accessToken 조회 (Redis 캐시 우선, 없으면 발급)
     */
    private String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cachedToken != null) {
            return cachedToken;
        }
        return refreshAccessToken();
    }

    /**
     * accessToken 재발급 및 Redis 캐싱
     */
    private String refreshAccessToken() {
        try {
            // clientId:clientSecret → Base64 인코딩
            String credentials = properties.getClientId() + ":" + properties.getClientSecret();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            String responseBody = restClient.post()
                    .uri(properties.getOauthUrl())
                    .header("Authorization", "Basic " + encodedCredentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials&scope=read")
                    .retrieve()
                    .body(String.class);

            JsonNode tokenResponse = objectMapper.readTree(responseBody);
            String accessToken = tokenResponse.get("access_token").asText();

            // Redis에 캐싱 (6일 TTL)
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, accessToken, TOKEN_CACHE_TTL_DAYS, TimeUnit.DAYS);

            log.info("코데프 accessToken 발급 완료");
            return accessToken;
        } catch (Exception e) {
            log.error("코데프 accessToken 발급 실패: {}", e.getMessage());
            throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
        }
    }
}
