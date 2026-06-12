package com.sofit.admin.domain.dev.converter;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryItemResponse;
import com.sofit.common.entity.sGrade.enums.BatchStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Spring Batch 메타데이터 → 배치 이력 조회 DTO 변환
 */
public class LoanDecisionBatchConverter {

    private LoanDecisionBatchConverter() {
    }

    /**
     * BATCH_JOB_EXECUTION 테이블 조회 결과를 DTO로 변환한다.
     */
    public static BatchHistoryItemResponse toBatchHistoryItemResponse(
            Long id,
            String springBatchStatus,
            int writeCount,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String exitMessage
    ) {
        BatchStatus status = convertStatus(springBatchStatus);
        Integer processedCount = writeCount;
        Long elapsedSeconds = calculateElapsedSeconds(startTime, endTime);
        String errorMessage = (exitMessage != null && !exitMessage.isBlank()) ? exitMessage : null;

        return new BatchHistoryItemResponse(
                id,
                status,
                processedCount,
                elapsedSeconds,
                errorMessage,
                startTime,
                endTime
        );
    }

    /**
     * Spring Batch STATUS 문자열 → 커스텀 BatchStatus 변환
     */
    private static BatchStatus convertStatus(String status) {
        if (status == null) {
            return BatchStatus.RUNNING;
        }
        return switch (status) {
            case "COMPLETED" -> BatchStatus.COMPLETED;
            case "FAILED", "ABANDONED", "UNKNOWN" -> BatchStatus.FAILED;
            default -> BatchStatus.RUNNING; // STARTING, STARTED, STOPPING, STOPPED
        };
    }

    private static Long calculateElapsedSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime).getSeconds();
    }
}
