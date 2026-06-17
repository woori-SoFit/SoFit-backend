package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.common.entity.sGrade.BatchExecutionHistory;
import com.sofit.common.entity.sGrade.enums.BatchStatus;
import com.sofit.common.repository.sGrade.BatchExecutionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevBatchServiceImpl 단위 테스트")
class DevBatchServiceImplTest {

    @InjectMocks
    private DevBatchServiceImpl devBatchService;

    @Mock
    private BatchExecutionHistoryRepository batchExecutionHistoryRepository;

    @Nested
    @DisplayName("findBatchHistories")
    class FindBatchHistoriesTest {

        @Test
        @DisplayName("page와 size가 null이면 기본값(0, 5)을 사용한다")
        void shouldUseDefaultValuesWhenPageAndSizeAreNull() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, null);

            // then
            assertThat(response.currentPage()).isZero();
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalCount()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.contents()).isEmpty();
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }

        @Test
        @DisplayName("page가 음수이면 기본값 0을 사용한다")
        void shouldUseDefaultPageWhenNegative() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(-1, null);

            // then
            assertThat(response.currentPage()).isZero();
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }

        @Test
        @DisplayName("size가 0이면 기본값 5를 사용한다")
        void shouldUseDefaultSizeWhenZero() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, 0);

            // then
            assertThat(response.size()).isEqualTo(5);
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }

        @Test
        @DisplayName("유효한 page와 size를 전달하면 해당 값으로 조회한다")
        void shouldUseProvidedPageAndSize() {
            // given
            Pageable expectedPageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(2, 10);

            // then
            assertThat(response.currentPage()).isEqualTo(2);
            assertThat(response.size()).isEqualTo(10);
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }

        @Test
        @DisplayName("배치 이력이 존재하면 목록을 정상 반환한다")
        void shouldReturnBatchHistoryList() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));

            BatchExecutionHistory history = mock(BatchExecutionHistory.class);
            given(history.getExecutionId()).willReturn(1L);
            given(history.getStatus()).willReturn(BatchStatus.COMPLETED);
            given(history.getSuccessCount()).willReturn(10);
            given(history.getErrorMessage()).willReturn(null);
            given(history.getStartedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0, 0));
            given(history.getCompletedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 5, 30));

            Page<BatchExecutionHistory> page = new PageImpl<>(List.of(history), expectedPageable, 1);
            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(page);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, null);

            // then
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).id()).isEqualTo(1L);
            assertThat(response.contents().get(0).status()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(response.contents().get(0).processedCount()).isEqualTo(10);
            assertThat(response.contents().get(0).elapsedSeconds()).isEqualTo(330L);
            assertThat(response.contents().get(0).errorMessage()).isNull();
            assertThat(response.contents().get(0).startedAt()).isEqualTo(LocalDateTime.of(2025, 6, 1, 10, 0, 0));
            assertThat(response.contents().get(0).finishedAt()).isEqualTo(LocalDateTime.of(2025, 6, 1, 10, 5, 30));
        }

        @Test
        @DisplayName("completedAt이 null이면 elapsedSeconds는 null이다")
        void shouldReturnNullElapsedSecondsWhenCompletedAtIsNull() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));

            BatchExecutionHistory history = mock(BatchExecutionHistory.class);
            given(history.getExecutionId()).willReturn(2L);
            given(history.getStatus()).willReturn(BatchStatus.RUNNING);
            given(history.getSuccessCount()).willReturn(0);
            given(history.getErrorMessage()).willReturn(null);
            given(history.getStartedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 11, 0, 0));
            given(history.getCompletedAt()).willReturn(null);

            Page<BatchExecutionHistory> page = new PageImpl<>(List.of(history), expectedPageable, 1);
            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(page);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, null);

            // then
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).elapsedSeconds()).isNull();
            assertThat(response.contents().get(0).status()).isEqualTo(BatchStatus.RUNNING);
        }

        @Test
        @DisplayName("실패한 배치 이력이면 errorMessage를 반환한다")
        void shouldReturnErrorMessageForFailedBatch() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));

            BatchExecutionHistory history = mock(BatchExecutionHistory.class);
            given(history.getExecutionId()).willReturn(3L);
            given(history.getStatus()).willReturn(BatchStatus.FAILED);
            given(history.getSuccessCount()).willReturn(5);
            given(history.getErrorMessage()).willReturn("AI 서버 연결 실패");
            given(history.getStartedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 12, 0, 0));
            given(history.getCompletedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 12, 1, 0));

            Page<BatchExecutionHistory> page = new PageImpl<>(List.of(history), expectedPageable, 1);
            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(page);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, null);

            // then
            assertThat(response.contents()).hasSize(1);
            assertThat(response.contents().get(0).status()).isEqualTo(BatchStatus.FAILED);
            assertThat(response.contents().get(0).errorMessage()).isEqualTo("AI 서버 연결 실패");
            assertThat(response.contents().get(0).elapsedSeconds()).isEqualTo(60L);
        }

        @Test
        @DisplayName("여러 건의 배치 이력을 페이징하여 반환한다")
        void shouldReturnPaginatedResults() {
            // given
            Pageable expectedPageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "executionId"));

            BatchExecutionHistory history1 = mock(BatchExecutionHistory.class);
            given(history1.getExecutionId()).willReturn(3L);
            given(history1.getStatus()).willReturn(BatchStatus.COMPLETED);
            given(history1.getSuccessCount()).willReturn(20);
            given(history1.getErrorMessage()).willReturn(null);
            given(history1.getStartedAt()).willReturn(LocalDateTime.of(2025, 6, 3, 10, 0, 0));
            given(history1.getCompletedAt()).willReturn(LocalDateTime.of(2025, 6, 3, 10, 10, 0));

            BatchExecutionHistory history2 = mock(BatchExecutionHistory.class);
            given(history2.getExecutionId()).willReturn(2L);
            given(history2.getStatus()).willReturn(BatchStatus.COMPLETED);
            given(history2.getSuccessCount()).willReturn(15);
            given(history2.getErrorMessage()).willReturn(null);
            given(history2.getStartedAt()).willReturn(LocalDateTime.of(2025, 6, 2, 10, 0, 0));
            given(history2.getCompletedAt()).willReturn(LocalDateTime.of(2025, 6, 2, 10, 8, 0));

            // totalElements=5이므로 전체 3페이지 (size=2)
            Page<BatchExecutionHistory> page = new PageImpl<>(List.of(history1, history2), expectedPageable, 5);
            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(page);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(1, 2);

            // then
            assertThat(response.totalCount()).isEqualTo(5);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(2);
            assertThat(response.contents()).hasSize(2);
        }

        @Test
        @DisplayName("size가 음수이면 기본값 5를 사용한다")
        void shouldUseDefaultSizeWhenNegative() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(null, -3);

            // then
            assertThat(response.size()).isEqualTo(5);
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }

        @Test
        @DisplayName("page가 0이면 정상적으로 첫 페이지를 조회한다")
        void shouldWorkWithPageZero() {
            // given
            Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "executionId"));
            Page<BatchExecutionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            given(batchExecutionHistoryRepository.findAll(expectedPageable)).willReturn(emptyPage);

            // when
            BatchHistoryListResponse response = devBatchService.findBatchHistories(0, 5);

            // then
            assertThat(response.currentPage()).isZero();
            verify(batchExecutionHistoryRepository).findAll(expectedPageable);
        }
    }
}
