# 시스템 심사 배치 관리 API

## 개요
Spring Batch 메타데이터 테이블을 활용하여 대출 심사 배치(loanDecisionJob) 실행 이력을 조회하는 API를 개발한다.
기존 `/api/admin/dev/batch/s-grade` 응답 구조와 동일한 형태로 제공한다.

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-loan-decision-batch-history`
- 커밋: `[SOFIT-XXX] Feat: 대출 심사 배치 이력 조회 API 구현`

---

## Phase 1: 기존 trigger API URL 변경 + 이력 조회 API 추가

### 1-1. BatchTriggerController URL 변경
- `POST /api/admin/dev/batch/loan-decision` → `POST /api/admin/dev/batch/loan-decision/trigger`

### 1-2. 이력 조회 Service 구현
- `DevLoanDecisionBatchService` (interface + impl) 생성
- Spring Batch 메타데이터 테이블(`BATCH_JOB_EXECUTION`)에서 `loanDecisionJob` 이력 조회
- `JdbcTemplate` 또는 Spring Batch의 `JobExplorer`를 활용하여 조회
- 페이징 처리

### 1-3. Response DTO 생성
- 기존 `BatchHistoryItemResponse` 필드와 매핑:

| BatchHistoryItemResponse 필드 | Spring Batch 메타데이터 매핑 |
|---|---|
| `id` | `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID` |
| `status` | `BATCH_JOB_EXECUTION.STATUS` (COMPLETED/FAILED/STARTED 등) |
| `processedCount` | `BATCH_STEP_EXECUTION.WRITE_COUNT` (또는 null) |
| `elapsedSeconds` | `END_TIME - START_TIME` 계산 |
| `errorMessage` | `BATCH_JOB_EXECUTION.EXIT_MESSAGE` (실패 시) |
| `startedAt` | `BATCH_JOB_EXECUTION.START_TIME` |
| `finishedAt` | `BATCH_JOB_EXECUTION.END_TIME` |

### 1-4. Controller에 GET 엔드포인트 추가
- `GET /api/admin/dev/batch/loan-decision`
- Query params: `page` (기본 0), `size` (기본 5)
- DEV_ADMIN 권한 체크
- 응답: `BatchHistoryListResponse` 재사용 (기존 s-grade와 동일 구조)

### 1-5. ControllerDocs 인터페이스 업데이트
- Swagger 문서 추가

---

## 응답 예시

```json
{
  "isSuccess": true,
  "code": "DEV_BATCH2001",
  "message": "배치 실행 이력 조회 성공",
  "result": {
    "contents": [
      {
        "id": 15,
        "status": "COMPLETED",
        "processedCount": 3,
        "elapsedSeconds": 12,
        "errorMessage": null,
        "startedAt": "2025-06-12T05:00:00",
        "finishedAt": "2025-06-12T05:00:12"
      }
    ],
    "totalCount": 10,
    "totalPages": 2,
    "currentPage": 0,
    "size": 5
  }
}
```

---

## 기술 결정: JobExplorer 사용

Spring Batch가 제공하는 `JobExplorer` 인터페이스를 사용한다.
- `jobExplorer.getJobInstances("loanDecisionJob", ...)` → Job 인스턴스 조회
- `jobExplorer.getJobExecutions(jobInstance)` → 실행 이력 조회
- JdbcTemplate으로 직접 쿼리하는 것보다 Spring Batch API 표준에 맞고, 테이블 스키마 변경에 강건하다.

---

## 영향 범위
- `BatchTriggerController.java` — URL 변경 + GET 엔드포인트 추가
- `BatchTriggerControllerTest.java` — URL 변경 반영
- 신규 파일: Service interface/impl, Converter
- 기존 DTO 재사용: `BatchHistoryListResponse`, `BatchHistoryItemResponse`
