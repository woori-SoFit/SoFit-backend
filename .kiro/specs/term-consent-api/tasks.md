# Implementation Plan: Term Consent API

## Overview

약관 동의 API(`POST /api/terms/consents`)를 구현합니다. ConsentHistory 엔티티와 Repository를 sofit-common에 생성하고, DTO/Converter/Service/Controller를 sofit-user에 추가하여 약관 동의 이력 저장 기능을 완성합니다. 기존 Term 엔티티/TermService/TermController에 메서드를 추가하는 형태로 구현합니다.

## Tasks

- [x] 1. ConsentHistory 엔티티 및 Repository 생성 (sofit-common)
  - [x] 1.1 ConsentHistory 엔티티 생성
    - `sofit-common/src/main/java/com/sofit/common/entity/term/ConsentHistory.java` 생성
    - @Entity, @Table(name = "consent_history"), @Getter, @NoArgsConstructor(access = AccessLevel.PROTECTED), @EntityListeners(AuditingEntityListener.class) 적용
    - 필드: consentId(PK, IDENTITY), userId(Long, NOT NULL), termId(Long, NOT NULL), applicationId(Long, nullable), isConsented(Boolean, NOT NULL), consentedAt(LocalDateTime, @CreatedDate)
    - BaseEntity 상속하지 않음
    - @Builder 패턴으로 생성자 제공
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 1.2 ConsentHistoryRepository 인터페이스 생성
    - `sofit-common/src/main/java/com/sofit/common/repository/ConsentHistoryRepository.java` 생성
    - JpaRepository<ConsentHistory, Long> 상속
    - _Requirements: 7.1_

- [x] 2. DTO 및 ErrorCode/SuccessCode 생성 (sofit-user)
  - [x] 2.1 ConsentCreateRequest DTO 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/terms/dto/request/ConsentCreateRequest.java` 생성
    - class 타입, 필드: termType(@NotNull TermType), applicationId(Long, nullable), consents(@NotEmpty @Valid List<ConsentItem>)
    - 내부 static class ConsentItem: termId(@NotNull Long), isConsented(@NotNull Boolean)
    - @Getter, @NoArgsConstructor 적용
    - _Requirements: 2.1, 3.3_

  - [x] 2.2 ConsentCreateResponse DTO 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/terms/dto/response/ConsentCreateResponse.java` 생성
    - record 타입, 필드: termType(TermType), applicationId(Long), userId(Long), consents(List<ConsentItemResponse>)
    - 내부 record ConsentItemResponse: termId(Long), isConsented(Boolean), consentedAt(LocalDateTime)
    - _Requirements: 2.2, 7.3_

  - [x] 2.3 TermErrorCode enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/terms/exception/TermErrorCode.java` 생성
    - BaseErrorCode 구현, 값: TERM_NOT_FOUND(404, "TERM4041"), TERM_TYPE_MISMATCH(400, "TERM4001"), REQUIRED_TERM_NOT_CONSENTED(400, "TERM4002")
    - _Requirements: 3.1, 4.1, 5.1_

  - [x] 2.4 TermSuccessCode에 CONSENT_OK 추가
    - 기존 `TermSuccessCode.java`에 CONSENT_OK(HttpStatus.OK, "TERM2001", "약관 동의가 완료되었습니다.") 추가
    - _Requirements: 2.3_

- [x] 3. Converter 및 Service 구현 (sofit-user)
  - [x] 3.1 TermConverter에 변환 메서드 추가
    - 기존 `TermConverter.java`에 toConsentHistoryList(Long userId, ConsentCreateRequest request, Long applicationId) 메서드 추가
    - toConsentResponse(TermType termType, Long applicationId, Long userId, List<ConsentHistory> savedHistories) 메서드 추가
    - static 메서드, private 생성자 유지
    - _Requirements: 7.1, 7.3_

  - [x] 3.2 TermService 인터페이스에 createConsents 메서드 추가
    - 기존 `TermService.java`에 `ConsentCreateResponse createConsents(Long userId, ConsentCreateRequest request)` 메서드 시그니처 추가
    - _Requirements: 2.5_

  - [x] 3.3 TermServiceImpl에 createConsents 비즈니스 로직 구현
    - 기존 `TermServiceImpl.java`에 createConsents 메서드 구현
    - ConsentHistoryRepository, LoanApplicationRepository 의존성 주입 추가
    - @Transactional 어노테이션 적용
    - 검증 순서: (1) termRepository.findAllById → 존재 여부 검증, (2) termType 일치 검증, (3) 필수 약관 동의 검증, (4) applicationId 소유권 검증 (nullable)
    - 검증 통과 후 TermConverter.toConsentHistoryList → consentHistoryRepository.saveAll → TermConverter.toConsentResponse
    - _Requirements: 3.1, 3.2, 4.1, 4.2, 5.1, 5.2, 6.1, 7.1, 7.2, 8.1, 8.2, 8.3, 8.4_

- [x] 4. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Controller 및 Swagger Docs 구현 (sofit-user)
  - [x] 5.1 TermConsentControllerDocs 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/terms/controller/TermConsentControllerDocs.java` 생성
    - @Operation, @ApiResponse 등 Swagger 어노테이션 정의
    - createConsents 메서드 시그니처 선언
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 5.2 TermConsentController 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/terms/controller/TermConsentController.java` 생성
    - @RestController, @RequestMapping("/api/terms"), @RequiredArgsConstructor
    - TermConsentControllerDocs implements
    - @PostMapping("/consents") 엔드포인트: SecurityContextHolder에서 userId 추출, termService.createConsents 호출, ApiResponse.onSuccess(TermSuccessCode.CONSENT_OK, response) 반환
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 6.1_

- [x] 6. Checkpoint - 컴파일 및 기본 동작 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. 단위 테스트 작성
  - [x] 7.1 TermServiceImpl 단위 테스트 작성
    - `sofit-user/src/test/java/com/sofit/user/domain/terms/service/TermServiceImplTest.java` 생성
    - JUnit 5 + Mockito 사용, @ExtendWith(MockitoExtension.class)
    - 테스트 케이스: 존재하지 않는 termId → TERM_NOT_FOUND, termType 불일치 → TERM_TYPE_MISMATCH, 필수 약관 미동의 → REQUIRED_TERM_NOT_CONSENTED, applicationId 소유권 실패 → 예외, applicationId null → 검증 건너뛰기, 모든 검증 통과 → saveAll 호출 및 응답 반환
    - _Requirements: 3.1, 4.1, 5.1, 7.1, 8.1, 8.2, 8.3_

  - [x] 7.2 TermConverter 단위 테스트 작성
    - `sofit-user/src/test/java/com/sofit/user/domain/terms/converter/TermConverterTest.java` 생성
    - toConsentHistoryList 변환 정확성, toConsentResponse 변환 정확성 검증
    - _Requirements: 7.1, 7.3_

- [x] 8. Property-Based 테스트 작성 (jqwik)
  - [x] 8.1 Property 1: 유효 요청에 대한 응답 구조 보존 테스트
    - `sofit-user/src/test/java/com/sofit/user/domain/terms/service/TermServiceImplPropertyTest.java` 생성
    - **Property 1: 유효 요청에 대한 응답 구조 보존**
    - **Validates: Requirements 2.2, 7.3**

  - [x] 8.2 Property 2: 존재하지 않는 약관 거부 테스트
    - **Property 2: 존재하지 않는 약관 거부**
    - **Validates: Requirements 3.1**

  - [x] 8.3 Property 3: termType 일치 검증 테스트
    - **Property 3: termType 일치 검증**
    - **Validates: Requirements 4.1, 4.2**

  - [x] 8.4 Property 4: 필수 약관 동의 검증 테스트
    - **Property 4: 필수 약관 동의 검증**
    - **Validates: Requirements 5.1, 5.2**

  - [x] 8.5 Property 5: ConsentHistory 저장 무결성 테스트
    - **Property 5: ConsentHistory 저장 무결성**
    - **Validates: Requirements 6.1, 7.1**

  - [x] 8.6 Property 6: applicationId 소유권 검증 테스트
    - **Property 6: applicationId 소유권 검증**
    - **Validates: Requirements 8.1, 8.2, 8.4**

  - [x] 8.7 Property 7: 유효하지 않은 입력 거부 테스트
    - **Property 7: 유효하지 않은 입력 거부**
    - **Validates: Requirements 2.4, 3.3**

- [x] 9. 통합 테스트 작성
  - [x] 9.1 TermConsentController 통합 테스트 작성
    - `sofit-user/src/test/java/com/sofit/user/domain/terms/controller/TermConsentControllerIntegrationTest.java` 생성
    - @SpringBootTest + MockMvc 사용
    - 테스트 케이스: 정상 요청 → 200 + TERM2001, 유효성 실패 → 400, 존재하지 않는 약관 → 404, termType 불일치 → 400, 필수 약관 미동의 → 400
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 4.1, 5.1_

- [x] 10. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 Term 엔티티는 수정하지 않음 (다른 브랜치에서 구현됨)
- TermController, TermService, TermServiceImpl, TermConverter, TermSuccessCode는 이미 존재한다고 가정하고 메서드/필드를 추가하는 형태
- DTO Request는 class, Response는 record 타입 사용
- Converter는 static 메서드 + private 생성자 패턴
- Controller에 Swagger 어노테이션 직접 작성 금지 → ControllerDocs 인터페이스 분리
- 클래스 레벨 @Transactional(readOnly = true), 쓰기 메서드에 @Transactional 명시
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- 커밋 컨벤션: `[SOFIT-XX] Feat: 약관 동의 API 구현`
- 브랜치: `feat/SOFIT-XX-약관-동의-API`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2", "2.3"] },
    { "id": 1, "tasks": ["1.2", "2.4", "3.1"] },
    { "id": 2, "tasks": ["3.2", "3.3"] },
    { "id": 3, "tasks": ["5.1"] },
    { "id": 4, "tasks": ["5.2"] },
    { "id": 5, "tasks": ["7.1", "7.2"] },
    { "id": 6, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7"] },
    { "id": 7, "tasks": ["9.1"] }
  ]
}
```
