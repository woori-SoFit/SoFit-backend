package com.sofit.admin.domain.dev.converter;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryItemResponse;
import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.common.entity.sGrade.BatchExecutionHistory;
import com.sofit.common.entity.sGrade.enums.BatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DevBatchConverter 단위 테스트")
class DevBatchConverterTest {

    @Test
    @DisplayName("toBatchHistoryItemResponse - 완료된 배치 이력을 변환한다")
    void toBatchHistoryItemResponse_completed() {
        // given
        LocalDateTime startedAt = LocalDateTime.of(2025, 6, 1, 10, 0, 0);
        LocalDateTime completedAt = LocalDateTime.of(2025, 6, 1, 10, 2, 0);
        BatchExecutionHistory history = createHistory(1L, BatchStatus.COMPLETED, 50, startedAt, completedAt, null);

        // when
        BatchHistoryItemResponse response = DevBatchConverter.toBatchHistoryItemResponse(history);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(response.processedCount()).isEqualTo(50);
        assertThat(response.elapsedSeconds()).isEqualTo(120L);
        assertThat(response.errorMessage()).isNull();
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.finishedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("toBatchHistoryItemResponse - 실패한 배치 이력은 elapsedSeconds가 null이다")
    void toBatchHistoryItemResponse_failed_noCompletedAt() {
        // given
        LocalDateTime startedAt = LocalDateTime.of(2025, 6, 1, 10, 0, 0);
        BatchExecutionHistory history = createHistory(2L, BatchStatus.FAILED, 0, startedAt, null, "OOM 에러");

        // when
        BatchHistoryItemResponse response = DevBatchConverter.toBatchHistoryItemResponse(history);

        // then
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(BatchStatus.FAILED);
        assertThat(response.elapsedSeconds()).isNull();
        assertThat(response.errorMessage()).isEqualTo("OOM 에러");
    }

    @Test
    @DisplayName("toBatchHistoryListResponse - 페이징된 배치 이력 목록을 변환한다")
    void toBatchHistoryListResponse() {
        // given
        LocalDateTime startedAt = LocalDateTime.of(2025, 6, 1, 10, 0, 0);
        LocalDateTime completedAt = LocalDateTime.of(2025, 6, 1, 10, 1, 30);
        BatchExecutionHistory history = createHistory(1L, BatchStatus.COMPLETED, 30, startedAt, completedAt, null);

        Page<BatchExecutionHistory> page = new PageImpl<>(
                List.of(history), PageRequest.of(0, 5), 1);

        // when
        BatchHistoryListResponse response = DevBatchConverter.toBatchHistoryListResponse(page, 0, 5);

        // then
        assertThat(response.contents()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.contents().get(0).elapsedSeconds()).isEqualTo(90L);
    }

    @Test
    @DisplayName("toBatchHistoryListResponse - 빈 페이지를 변환한다")
    void toBatchHistoryListResponse_emptyPage() {
        // given
        Page<BatchExecutionHistory> page = new PageImpl<>(
                List.of(), PageRequest.of(0, 5), 0);

        // when
        BatchHistoryListResponse response = DevBatchConverter.toBatchHistoryListResponse(page, 0, 5);

        // then
        assertThat(response.contents()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0);
    }

    // ===================== 테스트 픽스처 =====================

    private BatchExecutionHistory createHistory(Long id, BatchStatus status, int processedCount,
                                                 LocalDateTime startedAt, LocalDateTime completedAt,
                                                 String errorMessage) {
        try {
            var constructor = BatchExecutionHistory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            BatchExecutionHistory history = constructor.newInstance();

            setField(history, "executionId", id);
            setField(history, "status", status);
            setField(history, "successCount", processedCount);
            setField(history, "startedAt", startedAt);
            setField(history, "completedAt", completedAt);
            setField(history, "errorMessage", errorMessage);

            return history;
        } catch (Exception e) {
            throw new RuntimeException("BatchExecutionHistory 테스트 데이터 생성 실패", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
