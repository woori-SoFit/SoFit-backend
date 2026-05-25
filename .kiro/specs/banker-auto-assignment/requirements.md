# Requirements Document

## Introduction

대출 신청 최종 제출(submit) 시 담당 은행원을 라운드로빈 방식으로 자동 배정하는 기능이다.
서버 이중화(AWS) 환경을 고려하여 Redis INCR 기반으로 인덱스를 관리하며, 활성 상태인 은행원(ADMIN_BANK_TELLER) 목록에서 순환 배정한다.

## Glossary

- **Banker_Assignment_Service**: Redis INCR 기반 라운드로빈 로직으로 담당 은행원을 배정하는 서비스 컴포넌트
- **Loan_Application**: 대출 신청 엔티티. DRAFT → SUBMITTED 상태 전환 시 담당 은행원이 배정됨
- **Loan_Application_Service**: 대출 신청의 생성, 조회, 제출을 처리하는 서비스 컴포넌트
- **Active_Banker**: UserRole이 ADMIN_BANK_TELLER이고 UserStatus가 ACTIVE인 사용자
- **Round_Robin_Index**: Redis에 저장되는 순환 배정 카운터 (키: `banker:assign:index`)
- **User_Repository**: 사용자 엔티티에 대한 데이터 접근 계층

## Requirements

### Requirement 1: 대출 제출 시 담당 은행원 자동 배정

**User Story:** As a 소상공인 고객, I want 대출 신청 최종 제출 시 담당 은행원이 자동으로 배정되기를, so that 별도의 수동 배정 과정 없이 심사가 즉시 시작될 수 있다.

#### Acceptance Criteria

1. WHEN 대출 신청이 최종 제출(POST /api/loan-applications/{applicationId}/submit)되면, THE Loan_Application_Service SHALL Banker_Assignment_Service를 호출하여 ACTIVE 상태이며 ADMIN_BANK_TELLER 역할을 가진 은행원 중 한 명을 담당 은행원으로 배정한다.
2. WHEN 담당 은행원이 배정되면, THE Loan_Application SHALL assignedBankerId 필드에 배정된 은행원의 userId를 저장한다.
3. WHEN 대출 신청이 SUBMITTED 상태로 전환되면, THE Loan_Application SHALL assignedBankerId 값이 반드시 존재한다 (NULL이 아님).
4. IF Banker_Assignment_Service 호출 시 배정 가능한 은행원이 존재하지 않거나 배정 처리에 실패하면, THEN THE Loan_Application_Service SHALL 제출 트랜잭션 전체를 롤백하고 배정 실패를 나타내는 에러 응답을 반환한다.

### Requirement 2: 라운드로빈 방식 균등 배정

**User Story:** As a 은행 관리자, I want 대출 신청 건이 활성 은행원들에게 균등하게 분배되기를, so that 특정 은행원에게 업무가 편중되지 않는다.

#### Acceptance Criteria

1. WHEN 배정 요청이 발생하면, THE Banker_Assignment_Service SHALL 활성 상태(ACTIVE)이고 역할이 ADMIN_BANK_TELLER인 은행원 목록을 조회한다.
2. WHEN 활성 은행원 목록이 조회되면, THE Banker_Assignment_Service SHALL Redis INCR 명령으로 Round_Robin_Index를 1 증가시킨다.
3. WHEN Round_Robin_Index가 증가되면, THE Banker_Assignment_Service SHALL (증가된 인덱스 값 % 활성 은행원 수) 연산으로 배정 대상 은행원을 결정한다.
4. THE Banker_Assignment_Service SHALL 활성 은행원 목록을 userId 오름차순(ASC)으로 정렬하여, 동일한 활성 은행원 목록과 동일한 인덱스 값에 대해 항상 동일한 은행원이 선택되도록 한다.
5. WHEN 배정 대상 은행원이 결정되면, THE Banker_Assignment_Service SHALL 해당 은행원의 userId를 반환한다.

### Requirement 3: Redis 기반 인덱스 관리

**User Story:** As a 시스템 운영자, I want 배정 인덱스가 서버 재시작이나 다중 서버 환경에서도 일관되게 유지되기를, so that 서버 이중화 환경에서도 균등 배정이 보장된다.

#### Acceptance Criteria

1. THE Banker_Assignment_Service SHALL Redis의 `banker:assign:index` 키에 배정 카운터를 저장하며, 해당 키에 TTL을 설정하지 않아 서버 재시작 시에도 값이 유지되도록 한다.
2. WHILE 다중 서버 환경에서 동시에 배정 요청이 발생하더라도, THE Banker_Assignment_Service SHALL Redis INCR 명령을 사용하여 각 요청에 서로 다른 인덱스 값을 원자적으로 할당한다.
3. WHEN `banker:assign:index` 키가 Redis에 존재하지 않는 상태에서 최초 배정 요청이 발생하면, THE Banker_Assignment_Service SHALL Redis INCR 명령을 통해 키를 자동 생성하고 값을 1로 초기화한다.
4. IF Redis 연결 실패 또는 INCR 명령 실행 중 예외가 발생하면, THEN THE Banker_Assignment_Service SHALL 은행원 배정을 수행하지 않고 예외를 상위 호출자에게 전파한다.

### Requirement 4: 활성 은행원 부재 시 예외 처리

**User Story:** As a 소상공인 고객, I want 배정 가능한 은행원이 없을 때 명확한 오류 메시지를 받기를, so that 시스템 장애 상황을 인지할 수 있다.

#### Acceptance Criteria

1. IF status가 ACTIVE인 ADMIN_BANK_TELLER 역할의 은행원이 0명이면, THEN THE Banker_Assignment_Service SHALL NO_AVAILABLE_BANKER 에러 코드와 함께 BaseException을 발생시킨다.
2. IF 은행원 배정 중 예외가 발생하면, THEN THE Loan_Application_Service SHALL @Transactional 롤백에 의해 대출 신청 상태를 DRAFT로 유지하고 SUBMITTED로 변경하지 않는다.
3. IF 은행원 배정 실패로 BaseException이 발생하면, THEN THE System SHALL HTTP 500 Internal Server Error 상태 코드와 NO_AVAILABLE_BANKER 에러 코드를 포함한 공통 응답 포맷으로 클라이언트에 응답한다.

### Requirement 5: LoanApplication 엔티티 확장

**User Story:** As a 개발자, I want LoanApplication 엔티티에 담당 은행원 정보를 저장할 수 있기를, so that 심사 단계에서 담당 은행원을 식별할 수 있다.

#### Acceptance Criteria

1. THE Loan_Application SHALL assignedBankerId 필드를 Long 타입, nullable로 보유하며, loan_application 테이블의 assigned_banker_id 컬럼(BIGINT, NULL 허용)에 매핑한다.
2. WHEN assignBanker(Long bankerId) 메서드가 호출되면, THE Loan_Application SHALL assignedBankerId 필드를 전달받은 bankerId 값으로 설정한다.
3. IF assignBanker 호출 시 bankerId가 null이면, THEN THE Loan_Application SHALL IllegalArgumentException을 발생시킨다.
4. WHILE Loan_Application의 상태가 DRAFT인 경우, THE Loan_Application SHALL assignedBankerId 값이 NULL이다.

### Requirement 6: UserRepository 은행원 조회 메서드

**User Story:** As a 개발자, I want 역할과 상태 기준으로 사용자를 조회할 수 있기를, so that 활성 은행원 목록을 효율적으로 가져올 수 있다.

#### Acceptance Criteria

1. THE User_Repository SHALL findByRoleAndStatus(UserRole role, UserStatus status) 메서드를 제공하여 해당 역할과 상태 조건에 맞는 사용자를 List<User> 타입으로 반환한다.
2. IF findByRoleAndStatus 메서드 호출 시 조건에 맞는 사용자가 존재하지 않으면, THEN THE User_Repository SHALL 빈 리스트(empty list)를 반환한다.
