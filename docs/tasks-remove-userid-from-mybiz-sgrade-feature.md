# my_biz_data / s_grade_feature 테이블 user_id 컬럼 제거

- 브랜치: `refactor/SOFIT-179-remove-userid-mybiz-sgrade-feature`
- 커밋: `[SOFIT-179] Refactor: my_biz_data, s_grade_feature 테이블 user_id 컬럼 제거`

---

## Phase 1: 엔티티 및 Repository 수정 (sofit-common)

### 1-1. MyBizData 엔티티에서 user 필드 제거
- 파일: `sofit-common/.../entity/mybiz/MyBizData.java`
- `@ManyToOne User user` + `@JoinColumn(name = "user_id")` 제거

### 1-2. MyBizDataRepository 메서드 변경
- 파일: `sofit-common/.../repository/mybiz/MyBizDataRepository.java`
- 기존 userId 기반 메서드 4개 삭제
- businessNumber 기반 메서드 4개 추가:
  - `findFirstByBusinessNumberOrderByReferenceMonthDesc(String businessNumber)`
  - `findByBusinessNumberAndReferenceMonth(String businessNumber, LocalDate referenceMonth)`
  - `findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(String businessNumber, LocalDate start, LocalDate end)`
  - `findReferenceMonthsByBusinessNumber(String businessNumber)` (JPQL)

---

## Phase 2: Service 수정 (sofit-user)

### 2-1. MyBizServiceImpl 수정
- 파일: `sofit-user/.../mybiz/service/MyBizServiceImpl.java`
- `BusinessProfileRepository` 의존성 추가
- `findDashboard(Long userId, String month)` 로직 변경:
  - userId → `BusinessProfile.businessNumber` 조회
  - businessNumber로 MyBizData 조회

### 2-2. LoanStepServiceImpl 수정
- 파일: `sofit-user/.../loan/service/LoanStepServiceImpl.java`
- `BusinessProfileRepository` 의존성 추가
- `processMybizData(Long userId, Long applicationId)` 로직 변경:
  - userId → `BusinessProfile.businessNumber` 조회
  - businessNumber로 최신 MyBizData 조회

---

## Phase 3: SQL 스크립트 수정

### 3-1. data_static.sql 수정
- 파일: `sofit-common/.../sql/data_static.sql`
- `CREATE TABLE s_grade_feature`에서:
  - `user_id BIGINT NOT NULL` 컬럼 삭제
  - `INDEX idx_s_grade_feature_user_created (user_id, created_at)` 삭제

### 3-2. data_dummy.sql 수정
- 파일: `sofit-common/.../sql/data_dummy.sql`
- `INSERT INTO my_biz_data`: `user_id` 컬럼명 및 값 제거
- `INSERT INTO s_grade_feature`: `user_id` 컬럼명 및 값 제거

---

## 확인 사항
- [ ] 컴파일 오류 없음
- [ ] 기존 admin-backend API 영향 없음 확인
- [ ] Python AI 배치 팀에 s_grade_feature INSERT 시 user_id 제거 공유 필요
