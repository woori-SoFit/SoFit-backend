# 테이블 명세 — 약관 (term)

## 테이블: `term`

| 컬럼명 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| term_id | BIGINT | PK, AUTO_INCREMENT | 약관 고유 ID |
| term_type | ENUM('PERSONAL_INFO','MYDATA','MYBIZDATA','LOAN_APPLICATION','LOAN_AGREEMENT') | NOT NULL | 약관 타입 |
| version | VARCHAR(20) | NOT NULL | 약관 버전 (예: v1.0) |
| title | VARCHAR(20) | NOT NULL | 약관 명 |
| file_url | VARCHAR(500) | NOT NULL | PDF 파일 상대 경로 (예: `/terms/loan_application_v1.0.pdf`) |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 현행 약관 여부 (FALSE면 이전 버전) |
| is_required | BOOLEAN | NOT NULL | 필수 여부 (FALSE면 선택) |
| effective_at | DATETIME | NOT NULL | 약관 시행 일시 |
| created_at | DATETIME | NOT NULL | 약관 등록 일시 (@CreatedDate) |

## 엔티티 매핑

- **엔티티 위치**: `sofit-common/src/main/java/com/sofit/common/entity/term/Term.java`
- **Enum 위치**: `sofit-common/src/main/java/com/sofit/common/entity/term/enums/TermType.java`
- **BaseEntity 상속 안 함** — `updated_at` 없음, `created_at`만 `@CreatedDate`로 관리
- **DDL**: 로컬은 `ddl-auto: update` 로 자동 생성. dev/prod는 `seed.sql` 수동 적용 후 `validate` 권장.
- **시드 데이터**: `.kiro/specs/terms-list/seed.sql` 참고

## 약관 본문 저장 방식

- 약관 전문은 **PDF로 별도 저장**하며 DB에는 본문 텍스트를 저장하지 않는다.
- MVP 단계: `sofit-user/src/main/resources/static/terms/*.pdf` 에 수동 업로드. Spring Boot 정적 리소스 핸들러가 자동으로 서빙.
- DB의 `file_url`은 **상대 경로**만 저장 (예: `/terms/loan_application_v1.0.pdf`). 호스트 정보는 응답 생성 시 `sofit.storage.base-url` 설정값과 조립.
- 향후 AWS 이전 시: PDF만 S3로 이관하고 `sofit.storage.base-url`만 교체 → DB 데이터 변경 불필요.

## TermType Enum 값

| 값 | 설명 |
|---|---|
| PERSONAL_INFO | 개인정보수집 |
| MYDATA | 마이데이터 |
| MYBIZDATA | 마이비즈데이터 |
| LOAN_APPLICATION | 대출 신청 |
| LOAN_AGREEMENT | 대출 약정 체결 |
