# Implementation Plan: 담당 은행원 자동 배정 (Banker Auto-Assignment)

## Overview

대출 신청 최종 제출(submit) 시 Redis INCR 기반 라운드로빈으로 활성 은행원을 자동 배정하는 기능을 구현한다.
LoanApplication 엔티티 확장 → UserRepository 메서드 추가 → BankerAssignmentService 구현 → LoanApplicationServiceImpl 통합 순서로 진행한다.

## Tasks

- [ ] 1. LoanApplication 엔티티 확장 및 DB 마이그레이션
  - [ ] 1.1 LoanApplication 엔티티에 assignedBankerId 필드 및 assignBanker() 메서드 추가
    - `sofit-common/src/main/java/com/sofit/common/entity/loan/LoanApplication.java`에 `assignedBankerId` 필드(Long, nullable) 추가
    - `@Column(name = "assigned_banker_id")` 매핑
    - `assignBanker(Long bankerId)` 비즈니스 메서드 추가 (null 전달 시 IllegalArgumentException)
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 1.2 DB 마이그레이션 SQL 작성
    - `sofit-common/src/main/resources/db/migration/alter_loan_application_add_assigned_banker_id.sql` 생성
    - `ALTER TABLE loan_application ADD COLUMN assigned_banker_id BIGINT NULL COMMENT '담당 은행원 userId';`
    - _Requirements: 5.1_

- [ ] 2. UserRepository 메서드 추가 및 BankerAssignmentService 구현
  - [ ] 2.1 UserRepository에 findByRoleAndStatus 메서드 추가
    - `sofit-common/src/main/java/com/sofit/common/repository/user/UserRepository.java`에 메서드 추가
    - `List<User> findByRoleAndStatus(UserRole role, UserStatus status);`
    - _Requirements: 6.1, 6.2_

  - [ ] 2.2 LoanErrorCode에 NO_AVAILABLE_BANKER 에러 코드 추가
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/exception/LoanErrorCode.java`에 추가
    - `NO_AVAILABLE_BANKER(HttpStatus.INTERNAL_SERVER_ERROR, "LOAN5001", "배정 가능한 은행원이 없습니다.")`
    - _Requirements: 4.1, 4.3_

  - [ ] 2.3 BankerAssignmentService 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/service/BankerAssignmentService.java` 생성
    - `Long assignBanker()` 메서드 정의
    - _Requirements: 1.1, 2.5_

  - [ ] 2.4 BankerAssignmentServiceImpl 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/service/BankerAssignmentServiceImpl.java` 생성
    - Redis INCR 기반 라운드로빈 로직 구현
    - 활성 은행원 목록 조회 (ADMIN_BANK_TELLER, ACTIVE)
    - userId 오름차순 정렬 후 `index % size` 연산으로 대상 결정
    - 활성 은행원 0명 시 `BaseException(LoanErrorCode.NO_AVAILABLE_BANKER)` 발생
    - Redis 키: `banker:assign:index` (TTL 없음)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 4.1_

- [ ] 3. LoanApplicationServiceImpl 통합 및 응답 확장
  - [ ] 3.1 LoanApplicationServiceImpl의 submitApplication 메서드에 은행원 배정 로직 통합
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/service/LoanApplicationServiceImpl.java` 수정
    - `BankerAssignmentService` 의존성 주입
    - submit 처리 전에 `bankerAssignmentService.assignBanker()` 호출
    - 반환된 bankerId로 `application.assignBanker(bankerId)` 호출
    - 배정 실패 시 @Transactional 롤백으로 DRAFT 상태 유지
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.2_

  - [ ] 3.2 LoanApplicationSubmitResponse에 assignedBankerId 필드 추가
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/dto/response/LoanApplicationSubmitResponse.java` 수정
    - record에 `Long assignedBankerId` 필드 추가
    - _Requirements: 1.2_

  - [ ] 3.3 LoanApplicationConverter의 toSubmitResponse에 assignedBankerId 매핑 추가
    - `sofit-user/src/main/java/com/sofit/user/domain/loan/converter/LoanApplicationConverter.java` 수정
    - `application.getAssignedBankerId()` 값을 응답에 포함
    - _Requirements: 1.2_

- [ ] 4. Checkpoint - 컴파일 및 기본 동작 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. 테스트 작성
  - [ ]* 5.1 BankerAssignmentServiceImpl 단위 테스트 작성
    - `sofit-user/src/test/java/com/sofit/user/domain/loan/service/BankerAssignmentServiceImplTest.java` 생성
    - Mock: UserRepository, StringRedisTemplate
    - 테스트 케이스: 정상 배정, 활성 은행원 0명 예외, Redis 예외 전파
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 4.1_

  - [ ]* 5.2 Property Test - 라운드로빈 결정론적 배정 (Property 1)
    - **Property 1: 라운드로빈 결정론적 배정**
    - **Validates: Requirements 2.3, 2.4**
    - `sofit-user/src/test/java/com/sofit/user/domain/loan/service/BankerAssignmentPropertyTest.java` 생성
    - jqwik 사용: 임의의 User 리스트(1~50명) + 임의의 Long 인덱스 값으로 동일 입력 → 동일 결과 검증

  - [ ]* 5.3 Property Test - 배정 결과 유효성 (Property 2)
    - **Property 2: 배정 결과 유효성**
    - **Validates: Requirements 1.1, 2.1, 2.5**
    - 반환된 bankerId가 ACTIVE + ADMIN_BANK_TELLER 은행원 userId 집합에 포함되는지 검증

  - [ ]* 5.4 Property Test - 상태-배정 불변 조건 (Property 3)
    - **Property 3: 상태-배정 불변 조건**
    - **Validates: Requirements 1.3, 5.4**
    - `sofit-user/src/test/java/com/sofit/user/domain/loan/entity/LoanApplicationPropertyTest.java` 생성
    - DRAFT 상태 → assignedBankerId null, SUBMITTED 상태 → assignedBankerId non-null 검증

  - [ ]* 5.5 Property Test - assignBanker 설정 정확성 (Property 4)
    - **Property 4: assignBanker 설정 정확성**
    - **Validates: Requirements 5.2**
    - 임의의 non-null Long 값으로 assignBanker 호출 후 getAssignedBankerId 동일 값 반환 검증

  - [ ]* 5.6 LoanApplicationServiceImpl submitApplication 단위 테스트 확장
    - `sofit-user/src/test/java/com/sofit/user/domain/loan/service/LoanApplicationServiceImplTest.java` 생성/수정
    - Mock: BankerAssignmentService
    - 테스트 케이스: submit 성공 시 assignedBankerId 저장, 배정 실패 시 예외 전파
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 6. 테스트 의존성 추가
  - [ ] 6.1 sofit-user/build.gradle에 jqwik 테스트 의존성 추가
    - `testImplementation 'net.jqwik:jqwik:1.8.2'`
    - _Requirements: Testing Strategy_

- [ ] 7. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 requirements를 참조하여 추적 가능
- Checkpoints에서 컴파일 오류 및 테스트 통과 여부를 확인
- Property tests는 jqwik 라이브러리를 사용하여 correctness properties를 검증
- Unit tests는 Mockito 기반으로 특정 시나리오를 검증
- Redis INCR은 트랜잭션 롤백 대상이 아니므로, 카운터 gap이 발생할 수 있으나 균등 배정에는 영향 없음

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "2.2", "6.1"] },
    { "id": 1, "tasks": ["2.3", "2.4"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3"] },
    { "id": 3, "tasks": ["5.1", "5.4", "5.5"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.6"] }
  ]
}
```
