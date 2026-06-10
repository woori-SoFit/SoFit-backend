package com.sofit.user.domain.sgrade.service;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
import com.sofit.user.domain.sgrade.client.SGradeAiClient;
import com.sofit.user.domain.sgrade.dto.SGradePredictResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SGradeServiceImpl implements SGradeService {

    private final SGradeAiClient sGradeAiClient;
    private final SGradePersistenceService sGradePersistenceService;
    private final BusinessProfileRepository businessProfileRepository;
    private final MyBizDataRepository myBizDataRepository;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Async("sGradeExecutor")
    @Override
    public void predictAsync(Long userId, Long sGradeId) {
        log.info("[SGrade] 비동기 S등급 산출 시작 - userId={}, sGradeId={}", userId, sGradeId);

        // 1. userId → businessNumber → 최신 bizDataId 조회
        Optional<Long> bizDataIdOpt = resolveBizDataId(userId);
        if (bizDataIdOpt.isEmpty()) {
            log.warn("[SGrade] bizDataId 조회 실패 - userId={}", userId);
            sGradePersistenceService.markFailed(sGradeId);
            return;
        }
        Long bizDataId = bizDataIdOpt.get();

        // 2. AI 서버 호출 (최대 3회 재시도)
        SGradePredictResponse response = callWithRetry(bizDataId);

        // 3. 결과 저장
        if (response != null) {
            sGradePersistenceService.saveResult(sGradeId, bizDataId, response);
            log.info("[SGrade] S등급 산출 완료 - userId={}, grade={}", userId, response.sGrade());
        } else {
            sGradePersistenceService.markFailed(sGradeId);
            log.warn("[SGrade] S등급 산출 실패 (3회 재시도 초과) - userId={}, sGradeId={}", userId, sGradeId);
        }
    }

    private Optional<Long> resolveBizDataId(Long userId) {
        return businessProfileRepository.findByUser_UserId(userId)
                .flatMap(profile -> myBizDataRepository
                        .findFirstByBusinessNumberOrderByReferenceMonthDesc(profile.getBusinessNumber()))
                .map(MyBizData::getBizDataId);
    }

    private SGradePredictResponse callWithRetry(Long bizDataId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return sGradeAiClient.predict(bizDataId);
            } catch (Exception e) {
                log.warn("[SGrade] AI 서버 호출 실패 (시도 {}/{}) - bizDataId={}, error={}",
                        attempt, MAX_RETRIES, bizDataId, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep();
                }
            }
        }
        return null;
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
