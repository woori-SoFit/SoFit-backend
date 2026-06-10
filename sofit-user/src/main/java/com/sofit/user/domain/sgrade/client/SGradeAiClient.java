package com.sofit.user.domain.sgrade.client;

import com.sofit.user.domain.sgrade.dto.SGradePredictRequest;
import com.sofit.user.domain.sgrade.dto.SGradePredictResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SGradeAiClient {

    private final RestClient aiServerRestClient;

    /**
     * Python AI 서버에 S등급 예측을 요청한다.
     *
     * @param bizDataId s_grade_feature 조회용 biz_data_id
     * @return 예측 결과 (등급, 키워드, 조언 등)
     * @throws RestClientException 네트워크 오류 또는 서버 에러 시
     */
    public SGradePredictResponse predict(Long bizDataId) {
        SGradePredictRequest request = new SGradePredictRequest(bizDataId);

        return aiServerRestClient.post()
                .uri("/api/s-grade/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SGradePredictResponse.class);
    }
}
