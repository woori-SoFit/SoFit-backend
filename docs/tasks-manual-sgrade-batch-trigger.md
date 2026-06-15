# 수동 S등급 배치 실행 API 구현

## 브랜치/커밋 정보
- **브랜치**: `feat/SOFIT-XXX-manual-sgrade-batch`
- **커밋**: `[SOFIT-XXX] Feat: 수동 S등급 배치 실행 API 구현`

---

## Phase 1: 에러/성공 코드 + DTO 생성

### 파일 목록
1. `sofit-admin/.../domain/dev/exception/DevBatchErrorCode.java` — 도메인 전용 에러 코드
2. `sofit-admin/.../domain/dev/exception/DevBatchSuccessCode.java` — 도메인 전용 성공 코드
3. `sofit-admin/.../domain/dev/dto/request/BatchTriggerRequest.java` — 수동 배치 실행 요청 DTO
4. `sofit-admin/.../domain/dev/dto/response/BatchTriggerResponse.java` — 수동 배치 실행 응답 DTO
5. `sofit-admin/.../domain/dev/dto/response/BatchStatusResponse.java` — 배치 상태 조회 응답 DTO

### 에러 코드 정의
| 코드 | HTTP Status | 메시지 |
|------|------------|--------|
| `BATCH4091` | 409 Conflict | 이미 배치가 실행 중입니다. |
| `BATCH5031` | 503 Service Unavailable | AI 서버 연결에 실패했습니다. |

### 성공 코드 정의
| 코드 | HTTP Status | 메시지 |
|------|------------|--------|
| `BATCH2021` | 202 Accepted | 배치 실행이 시작되었습니다. |
| `BATCH2001` | 200 OK | 배치 상태 조회에 성공했습니다. |

---

## Phase 2: AI Server 클라이언트 생성

### 파일 목록
1. `sofit-admin/.../domain/dev/client/SGradeBatchClient.java` — AI 서버 배치 관련 HTTP 호출 클라이언트

### 기능
- `triggerBatch(Long triggeredBy)` → POST /api/s-grade/batch?triggered_by={userId}
- `getBatchStatus()` → GET /api/s-grade/batch/status
- connect timeout 3초, read timeout 10초
- 연결 실패 시 BaseException(BATCH5031) throw

---

## Phase 3: Service 구현

### 파일 목록
1. `sofit-admin/.../domain/dev/service/DevBatchService.java` — 메서드 추가
2. `sofit-admin/.../domain/dev/service/DevBatchServiceImpl.java` — 구현 추가

### 메서드
- `triggerSGradeBatch(Long triggeredBy)` → 상태 확인 후 배치 트리거
- `getSGradeBatchStatus()` → AI 서버 상태 조회 프록시

### 비즈니스 로직
- POST 전에 GET /status로 RUNNING 여부 확인 → RUNNING이면 409 Conflict

---

## Phase 4: Controller + Docs 인터페이스 구현

### 파일 목록
1. `sofit-admin/.../domain/dev/controller/DevBatchControllerDocs.java` — Swagger 메서드 추가
2. `sofit-admin/.../domain/dev/controller/DevBatchController.java` — 엔드포인트 추가

### 엔드포인트
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/admin/dev/batch/s-grade/trigger` | ADMIN_DEV | 수동 배치 실행 |
| GET | `/api/admin/dev/batch/s-grade/status` | ADMIN_DEV | 배치 상태 조회 |

---

## Phase 5: 컴파일 확인 + 테스트 JSON + PR 본문
