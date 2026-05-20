# Requirements Document

## Introduction

사용자가 약관에 동의한 이력을 저장하는 API를 구현합니다. 기존 `feat/SOFIT-45-약관-목록-조회-API` 브랜치에 구현된 Term 엔티티/리포지토리를 기반으로, ConsentHistory 엔티티와 약관 동의 POST API를 추가합니다. 약관 동의 시 필수 약관 미동의 검증, termType 일치 검증, 약관 존재 여부 검증을 수행합니다.

## Glossary

- **Term_Consent_API**: 사용자가 약관에 동의한 이력을 저장하는 REST API 엔드포인트 (`POST /api/terms/consents`)
- **ConsentHistory**: 사용자의 약관 동의 이력을 저장하는 JPA 엔티티 (테이블명: `consent_history`)
- **Term**: 약관 정보를 관리하는 기존 JPA 엔티티 (termId, termType, version, title, fileUrl, isActive, isRequired, effectiveAt, createdAt)
- **TermType**: 약관 유형을 나타내는 enum (PERSONAL_INFO, MYDATA, MYBIZDATA, LOAN_APPLICATION, LOAN_AGREEMENT)
- **TermRepository**: Term 엔티티에 대한 JPA Repository
- **ConsentHistoryRepository**: ConsentHistory 엔티티에 대한 JPA Repository
- **TermService**: 약관 관련 비즈니스 로직을 처리하는 서비스 인터페이스
- **TermServiceImpl**: TermService의 구현체
- **TermConverter**: Term/ConsentHistory 엔티티와 DTO 간 변환을 담당하는 클래스
- **SecurityContext**: Spring Security에서 현재 인증된 사용자 정보를 제공하는 컨텍스트

## Requirements

### Requirement 1: ConsentHistory 엔티티 생성

**User Story:** As a 개발자, I want ConsentHistory 엔티티를 정의하여, 사용자의 약관 동의 이력을 데이터베이스에 영속화할 수 있다.

#### Acceptance Criteria

1. THE ConsentHistory entity SHALL have consent_id(BIGINT, PK, AUTO_INCREMENT), user_id(BIGINT, NOT NULL), term_id(BIGINT, NOT NULL), application_id(BIGINT, NULL 허용), is_consented(BOOLEAN, NOT NULL), consented_at(DATETIME, NOT NULL) 컬럼을 포함하며, @Table(name = "consent_history")로 매핑한다
2. THE ConsentHistory entity SHALL user_id를 Long userId 필드로 선언하고 @Column(name = "user_id", nullable = false)로 매핑한다 (@ManyToOne 관계 사용하지 않음)
3. THE ConsentHistory entity SHALL term_id를 Long termId 필드로 선언하고 @Column(name = "term_id", nullable = false)로 매핑한다 (@ManyToOne 관계 사용하지 않음)
4. THE ConsentHistory entity SHALL application_id를 Long applicationId 필드로 선언하고 @Column(name = "application_id")으로 매핑한다 (nullable 허용, @ManyToOne 관계 사용하지 않음)
5. THE ConsentHistory entity SHALL BaseEntity를 상속하지 않고, @EntityListeners(AuditingEntityListener.class)를 클래스에 직접 선언하여 consented_at 필드에 @CreatedDate를 적용한다
6. THE ConsentHistory entity SHALL @Entity, @Table, @Getter, @NoArgsConstructor(access = AccessLevel.PROTECTED) 어노테이션을 사용하고, @Id @GeneratedValue(strategy = GenerationType.IDENTITY)로 consent_id를 PK로 설정한다

### Requirement 2: 약관 동의 API 엔드포인트

**User Story:** As a 소상공인 고객, I want 약관 동의 API를 호출하여, 약관 동의 이력을 서버에 저장할 수 있다.

#### Acceptance Criteria

1. WHEN POST /api/terms/consents 요청이 수신되면, THE Term_Consent_API SHALL termType(TermType enum, @NotNull), applicationId(Long, nullable), consents(@NotEmpty, 각 항목은 termId(@NotNull)와 isConsented(@NotNull) 포함) 필드를 포함하는 요청 본문을 수신한다
2. WHEN 약관 동의 요청이 정상 처리되면, THE Term_Consent_API SHALL HTTP 200 상태코드와 함께 termType, applicationId, userId, consents(각 항목은 termId, isConsented, consentedAt(ISO 8601 형식) 포함) 목록을 포함하는 응답을 반환한다
3. THE Term_Consent_API SHALL ApiResponse.onSuccess 래퍼를 사용하여 응답 코드로 "TERM2001"을, 메시지로 "약관 동의가 완료되었습니다."를 반환한다
4. IF 요청 본문의 필수 필드(@NotNull, @NotEmpty)가 누락되거나 유효하지 않으면, THEN THE Term_Consent_API SHALL HTTP 400 상태코드와 함께 유효성 검증 실패를 나타내는 에러 응답을 반환한다
5. WHEN 약관 동의 요청이 수신되면, THE Term_Consent_API SHALL SecurityContext에서 인증된 사용자 정보를 추출하여 요청을 처리한다

### Requirement 3: 약관 존재 여부 검증

**User Story:** As a 소상공인 고객, I want 존재하지 않는 약관에 대한 동의 요청이 거부되어, 잘못된 데이터가 저장되지 않도록 한다.

#### Acceptance Criteria

1. IF 요청된 termId 목록 중 하나 이상이 데이터베이스에 존재하지 않는다면, THEN THE TermServiceImpl SHALL 전체 요청을 거부하고 TERM_NOT_FOUND 예외(HTTP 404, 코드 "TERM4041")를 발생시킨다
2. WHEN 약관 동의 요청을 수신하면, THE TermServiceImpl SHALL 요청된 모든 termId에 대해 termRepository.findAllById(termIds)를 사용하여 데이터베이스 존재 여부를 일괄 조회하여 검증한다
3. IF 요청된 termId 목록이 비어 있다면, THEN THE Term_Consent_API SHALL 유효성 검증 실패 예외(HTTP 400)를 발생시킨다

### Requirement 4: 약관 유형 일치 검증

**User Story:** As a 소상공인 고객, I want 요청한 termType과 실제 약관의 termType이 일치하는지 검증되어, 잘못된 유형의 약관에 동의하는 것을 방지한다.

#### Acceptance Criteria

1. IF 요청된 termIds로 조회된 약관들 중 하나라도 요청의 termType과 일치하지 않는 termType을 가지면, THEN THE TermServiceImpl SHALL TERM_TYPE_MISMATCH 예외(HTTP 400, 코드 "TERM4001")를 발생시킨다
2. WHEN 요청된 termIds로 조회된 모든 약관의 termType이 요청의 termType과 일치하면, THE TermServiceImpl SHALL 검증을 통과시키고 후속 동의 처리를 계속 진행한다

### Requirement 5: 필수 약관 동의 검증

**User Story:** As a 소상공인 고객, I want 필수 약관에 대해 미동의 시 요청이 거부되어, 서비스 이용에 필요한 약관 동의가 보장된다.

#### Acceptance Criteria

1. WHEN 조회된 약관 중 isRequired가 true인 약관에 대해 요청 consents 배열의 해당 termId 항목의 isConsented가 false이면, THE TermServiceImpl SHALL REQUIRED_TERM_NOT_CONSENTED 예외(HTTP 400, 코드 "TERM4002")를 발생시킨다
2. WHEN 조회된 약관 중 isRequired가 false인 약관에 대해 isConsented가 false인 동의 항목이 포함되어 있으면, THE TermServiceImpl SHALL 예외를 발생시키지 않고 정상 처리한다

### Requirement 6: 인증된 사용자 식별

**User Story:** As a 소상공인 고객, I want 로그인된 상태에서 약관 동의를 수행하여, 동의 이력이 본인 계정에 정확히 기록된다.

#### Acceptance Criteria

1. WHEN 약관 동의 요청이 수신되면, THE TermServiceImpl SHALL SecurityContextHolder.getContext().getAuthentication()에서 현재 로그인된 사용자의 userId를 추출하여 ConsentHistory의 user_id 필드에 저장한다
2. IF SecurityContext에서 인증 정보가 null이거나 userId를 추출할 수 없으면, THEN THE TermServiceImpl SHALL 인증 실패 예외(HTTP 401, 코드 "COMMON4001")를 발생시킨다

### Requirement 7: 동의 이력 일괄 저장

**User Story:** As a 소상공인 고객, I want 여러 약관에 대한 동의를 한 번의 요청으로 처리하여, 효율적으로 약관 동의를 완료할 수 있다.

#### Acceptance Criteria

1. WHEN 요청된 동의 항목 리스트에 대해 약관 존재 여부, termType 일치, 필수 약관 동의 검증이 모두 통과되면, THE TermServiceImpl SHALL 각 동의 항목에 대해 ConsentHistory 엔티티를 생성하고 consentHistoryRepository.saveAll()로 일괄 저장한다
2. IF saveAll() 수행 중 예외가 발생하면, THEN THE TermServiceImpl SHALL @Transactional에 의해 모든 동의 이력 저장을 롤백하고 예외를 전파한다
3. THE TermConverter SHALL 저장된 ConsentHistory 엔티티 리스트를 각 항목의 termId, isConsented, consentedAt를 포함하는 응답 DTO(record 타입)로 변환한다

### Requirement 8: applicationId 연관 (선택적)

**User Story:** As a 소상공인 고객, I want 대출 신청과 연관된 약관 동의 시 applicationId를 함께 저장하여, 대출 신청별 약관 동의 이력을 추적할 수 있다.

#### Acceptance Criteria

1. WHEN applicationId가 요청에 포함되면, THE TermServiceImpl SHALL LoanApplicationRepository를 통해 해당 applicationId와 현재 사용자의 userId로 LoanApplication의 존재 여부와 소유권을 검증한다
2. IF applicationId에 해당하는 LoanApplication이 존재하지 않거나 현재 사용자의 소유가 아니면, THEN THE TermServiceImpl SHALL BaseException을 발생시키며, 해당 LoanApplication을 찾을 수 없음을 나타내는 ErrorCode를 포함한다
3. IF applicationId가 null이면, THEN THE TermServiceImpl SHALL applicationId 검증을 건너뛰고 ConsentHistory의 application_id를 null로 저장한다
4. WHEN applicationId 검증이 성공하면, THE TermServiceImpl SHALL ConsentHistory의 application_id 필드에 해당 applicationId 값을 저장한다
