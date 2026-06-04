# 성장 S등급 배치 관리 조회 API - tasks.md

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-s-grade-batch-history`
- 커밋: `[SOFIT-XXX] Feat: 성장 S등급 배치 실행 이력 조회 API 구현`

---

## Phase 1: Entity + Enum + Repository

### 작업 내용
1. Enum 생성 (위치: `sofit-admin/.../domain/dev/entity/enums/`)
   - `ExecutionType`: AUTO, MANUAL
   - `ExecutionCycle`: DAILY, MONTHLY
   - `BatchStatus`: RUNNING, COMPLETED, FAILED

2. `BatchExecutionHistory` 엔티티 생성 (BaseEntity 상속 안 함)
   - 위치: `sofit-admin/.../domain/dev/entity/BatchExecutionHistory.java`
   - 테이블: `batch_execution_history`
   - 컬럼 매핑:
     - `execution_id` → Long (PK, GeneratedValue)
     - `execution_type` → ExecutionType (ENUM)
     - `execution_cycle` → ExecutionCycle (ENUM)
     - `triggered_by` → Long (nullable)
     - `status` → BatchStatus (ENUM)
     - `total_count` → Integer
     - `success_count` → Integer
     - `fail_count` → Integer
     - `error_message` → String (nullable, TEXT)
     - `started_at` → LocalDateTime
     - `completed_at` → LocalDateTime (nullable)

3. `BatchExecutionHistoryRepository` 생성
   - 위치: `sofit-admin/.../domain/dev/repository/BatchExecutionHistoryRepository.java`
   - JpaRepository<BatchExecutionHistory, Long>

---

## Phase 2: DTO + Converter

### 작업 내용
1. `BatchHistoryItemResponse` (record)
   - 필드: id, status, processedCount, elapsedSeconds, errorMessage, startedAt, finishedAt

2. `BatchHistoryListResponse` (record)
   - 필드: contents, totalCount, totalPages, currentPage, size

3. `DevBatchConverter`
   - Entity → DTO 변환
   - elapsedSeconds: `completed_at - started_at`을 초(seconds) 단위로 계산 (completed_at이 null이면 null)

---

## Phase 3: Service

### 작업 내용
1. `DevBatchService` 인터페이스
2. `DevBatchServiceImpl` 구현체
   - 페이징: page 기본값 0, size 기본값 5
   - 정렬: started_at DESC

---

## Phase 4: Controller + ControllerDocs

### 작업 내용
1. `DevBatchControllerDocs` 인터페이스 (Swagger)
2. `DevBatchController`
   - GET `/api/admin/dev/batch/s-grade`
   - 권한 체크: ADMIN_DEV만 허용 → 불일치 시 GeneralErrorCode.FORBIDDEN

---

## 응답 매핑 (Response ↔ DB 컬럼)

| Response 필드 | DB 컬럼 | 비고 |
|---|---|---|
| id | execution_id | PK |
| status | status | ENUM → String |
| processedCount | success_count | 성공 건수 |
| elapsedSeconds | completed_at - started_at | 초 단위 변환, null 가능 |
| errorMessage | error_message | nullable |
| startedAt | started_at | |
| finishedAt | completed_at | DB 컬럼명 completed_at |
