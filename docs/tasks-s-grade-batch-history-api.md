# S등급 배치 연동 — Spring Boot 변경 작업

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-s-grade-batch-integration`
- 커밋: `[SOFIT-XXX] Feat: S등급 배치 연동을 위한 테이블 매핑 및 로직 변경`

---

## Phase 1: SQL 파일 테이블명 수정 (`s_input_feature` → `s_grade_feature`)

### 작업 내용
- [ ] `data_static.sql` — DDL 테이블명/인덱스명 변경
- [ ] `data_dummy.sql` — INSERT문 테이블명 및 주석 변경

### 대상 파일
- `sofit-user/src/main/resources/sql/data_static.sql`
- `sofit-user/src/main/resources/sql/data_dummy.sql`

---

## Phase 2: `SGradeHistoryRepository` 생성 + `SGradeHistory` 엔티티 생성 메서드 추가

### 작업 내용
- [ ] `SGradeHistoryRepository` 생성 (JpaRepository)
  - `findByUser_UserIdAndStatus(Long userId, SGradeStatus status)` — FAILED 상태 조회 (수동 배치 복구용)
- [ ] `SGradeHistory` 엔티티에 `createRequested(User user)` 정적 팩토리 메서드 추가
  - status = REQUESTED, requestedAt = NOW(), featureId = null, batchExecutionId = null

### 대상 파일
- `sofit-common/src/main/java/com/sofit/common/repository/sGrade/SGradeHistoryRepository.java` (신규)
- `sofit-common/src/main/java/com/sofit/common/entity/sGrade/SGradeHistory.java` (수정)

---

## Phase 3: 회원가입 완료 시 `SGradeHistory` REQUESTED INSERT

### 작업 내용
- [ ] `AuthServiceImpl.completeSignup()` 내부 트랜잭션에서 User 저장 후 `SGradeHistory.createRequested(user)` INSERT 추가

### 대상 파일
- `sofit-user/src/main/java/com/sofit/user/domain/auth/service/AuthServiceImpl.java`

---

## Phase 4: `SGradeReportRepository` 조회 쿼리에 COMPLETED 조건 추가

### 작업 내용
- [ ] 기존 `findTopByUser_UserIdOrderByCreatedAtDesc` → JPQL 커스텀 쿼리로 변경
  - `s_grade_history`와 JOIN하여 `status = COMPLETED`인 건만 조회
  - `s_grade_history.evaluated_at DESC` 기준 정렬

### 대상 파일
- `sofit-common/src/main/java/com/sofit/common/repository/sGrade/SGradeReportRepository.java`

---

## Phase 5: 코드 정리 (주석/변수명)

### 작업 내용
- [ ] `LoanApplicationRepository.java` — 주석 "s_evaluation_id" → "s_grade_id"
- [ ] `LoanApplicationGradeServiceImpl.java` — 변수명 `sEvaluationId` → `sGradeId`, 주석 수정
- [ ] `ReportConverter.java` — Javadoc "ShapExplanation 엔티티" → "SGradeReport 엔티티"

### 대상 파일
- `sofit-common/src/main/java/com/sofit/common/repository/loan/LoanApplicationRepository.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanApplicationGradeServiceImpl.java`
- `sofit-user/src/main/java/com/sofit/user/domain/report/converter/ReportConverter.java`
