package com.sofit.user.domain.sgrade.service;

import com.sofit.common.entity.sGrade.SGradeHistory;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.repository.sGrade.SGradeHistoryRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.user.domain.sgrade.converter.SGradeConverter;
import com.sofit.user.domain.sgrade.dto.SGradePredictResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * S등급 산출 결과 저장을 담당하는 별도 Bean.
 * SGradeServiceImpl에서 내부 호출 시 @Transactional 프록시가 적용되도록 분리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SGradePersistenceService {

    private final SGradeHistoryRepository sGradeHistoryRepository;
    private final SGradeReportRepository sGradeReportRepository;

    /**
     * S등급 산출 성공 시 history COMPLETED 업데이트 + report INSERT를 하나의 트랜잭션으로 처리한다.
     */
    @Transactional
    public void saveResult(Long sGradeId, Long bizDataId, SGradePredictResponse response) {
        SGradeHistory history = sGradeHistoryRepository.findById(sGradeId).orElse(null);
        if (history == null) {
            log.error("[SGrade] SGradeHistory 조회 실패 - sGradeId={}", sGradeId);
            return;
        }
        history.markCompleted(bizDataId);
        sGradeHistoryRepository.save(history);

        SGradeReport report = SGradeConverter.toSGradeReport(history, response);
        sGradeReportRepository.save(report);
    }

    /**
     * S등급 산출 실패 시 history 상태를 FAILED로 변경한다.
     */
    @Transactional
    public void markFailed(Long sGradeId) {
        SGradeHistory history = sGradeHistoryRepository.findById(sGradeId).orElse(null);
        if (history != null) {
            history.markFailed();
            sGradeHistoryRepository.save(history);
        }
    }
}
