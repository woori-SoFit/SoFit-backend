package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.converter.LoanDecisionBatchConverter;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryItemResponse;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Batch 메타데이터 테이블에서 loanDecisionJob 실행 이력을 조회하는 서비스.
 * JobExplorer 대신 JdbcTemplate으로 직접 조회하여 안정성을 확보한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanDecisionBatchServiceImpl implements LoanDecisionBatchService {

    private static final String JOB_NAME = "loanDecisionJob";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BatchHistoryListResponse findBatchHistories(Integer page, Integer size) {
        int actualPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int actualSize = (size != null && size >= 1) ? size : DEFAULT_SIZE;

        // 전체 건수 조회
        long totalCount = countExecutions();
        int totalPages = (int) Math.ceil((double) totalCount / actualSize);

        // 페이징된 이력 조회
        int offset = actualPage * actualSize;
        List<BatchHistoryItemResponse> contents = findExecutions(offset, actualSize);

        return new BatchHistoryListResponse(contents, totalCount, totalPages, actualPage, actualSize);
    }

    private long countExecutions() {
        String sql = """
                SELECT COUNT(*)
                FROM BATCH_JOB_EXECUTION e
                JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
                WHERE i.JOB_NAME = ?
                """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, JOB_NAME);
        return count != null ? count : 0;
    }

    private List<BatchHistoryItemResponse> findExecutions(int offset, int limit) {
        String sql = """
                SELECT e.JOB_EXECUTION_ID,
                       e.STATUS,
                       e.START_TIME,
                       e.END_TIME,
                       e.EXIT_CODE,
                       e.EXIT_MESSAGE,
                       COALESCE(s.WRITE_COUNT, 0) AS WRITE_COUNT
                FROM BATCH_JOB_EXECUTION e
                JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
                LEFT JOIN BATCH_STEP_EXECUTION s ON e.JOB_EXECUTION_ID = s.JOB_EXECUTION_ID
                WHERE i.JOB_NAME = ?
                ORDER BY e.JOB_EXECUTION_ID DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long id = rs.getLong("JOB_EXECUTION_ID");
            String status = rs.getString("STATUS");
            LocalDateTime startTime = rs.getObject("START_TIME", LocalDateTime.class);
            LocalDateTime endTime = rs.getObject("END_TIME", LocalDateTime.class);
            String exitMessage = rs.getString("EXIT_MESSAGE");
            int writeCount = rs.getInt("WRITE_COUNT");

            return LoanDecisionBatchConverter.toBatchHistoryItemResponse(
                    id, status, writeCount, startTime, endTime, exitMessage
            );
        }, JOB_NAME, limit, offset);
    }
}
