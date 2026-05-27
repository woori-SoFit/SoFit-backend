# tasks.md — SOFIT-45 약관 목록 조회 API

## 브랜치/커밋 정보

- **브랜치**: `feat/SOFIT-45-약관-목록-조회-API` (이미 checkout됨)
- **커밋 메시지**: `[SOFIT-45] Feat: 약관 목록 조회 API 구현`
- **PR 베이스**: `dev`

---

## Phase 1. sofit-common — 엔티티 / Enum / Repository

### 1-1. TermType Enum 생성

- **파일**: `sofit-common/src/main/java/com/sofit/common/entity/term/enums/TermType.java`
- 값: `PERSONAL_INFO`, `MYDATA`, `MYBIZDATA`, `LOAN_APPLICATION`, `LOAN_AGREEMENT`

### 1-2. Term 엔티티 생성

- **파일**: `sofit-common/src/main/java/com/sofit/common/entity/term/Term.java`
- `BaseEntity` 상속하지 않음
- `@EntityListeners(AuditingEntityListener.class)` 직접 선언
- `created_at`만 `@CreatedDate`로 관리 (`updated_at` 없음)
- `@Enumerated(EnumType.STRING)` 사용
- PK: `term_id` (BIGINT, `@GeneratedValue(strategy = GenerationType.IDENTITY)`)

### 1-3. TermRepository 생성

- **파일**: `sofit-common/src/main/java/com/sofit/common/repository/TermRepository.java`
- 메서드: `List<Term> findByTermTypeAndIsActiveTrue(TermType termType)`

---

## Phase 2. sofit-user — terms 도메인 구현

### 2-1. TermSuccessCode 생성

- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/terms/exception/TermSuccessCode.java`
- `TERM_LIST_OK(HttpStatus.OK, "TERM2000", "약관 목록 조회에 성공했습니다.")`

### 2-2. TermListResponse (record) 생성

- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/terms/dto/response/TermListResponse.java`
- 래핑 구조: `TermListResponse { List<TermItem> terms }`
- `TermItem` record: termId, termType, version, title, content, isRequired, isActive, effectiveAt

### 2-3. TermConverter 생성

- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/terms/converter/TermConverter.java`
- `toListResponse(List<Term> terms)` → `TermListResponse`

### 2-4. TermService 인터페이스 + TermServiceImpl 생성

- **인터페이스**: `sofit-user/src/main/java/com/sofit/user/domain/terms/service/TermService.java`
- **구현체**: `sofit-user/src/main/java/com/sofit/user/domain/terms/service/TermServiceImpl.java`
- 메서드: `TermListResponse findTerms(TermType termType)`
- Repository에서 `findByTermTypeAndIsActiveTrue` 호출 → Converter로 변환

### 2-5. TermControllerDocs 인터페이스 생성

- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/terms/controller/TermControllerDocs.java`
- Swagger 어노테이션 분리 (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`)

### 2-6. TermController 생성

- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/terms/controller/TermController.java`
- `@GetMapping` → `GET /api/terms`
- `@RequestParam TermType termType`
- 응답 변수명: `response`
- `implements TermControllerDocs`

---

## Phase 3. 검증 및 산출물

### 3-1. 컴파일 오류 확인

- `./gradlew :sofit-user:compileJava` 실행

### 3-2. Apidog 테스트 JSON 제공

- `GET http://localhost:8080/api/terms?termType=LOAN_APPLICATION`

### 3-3. PR 본문 제공

- workflow.md 템플릿 기반, `closes #SOFIT-45`

### 3-4. 커밋/브랜치 안내

- 커밋: `[SOFIT-45] Feat: 약관 목록 조회 API 구현`
- git 명령어 안내 (직접 실행 안 함)
