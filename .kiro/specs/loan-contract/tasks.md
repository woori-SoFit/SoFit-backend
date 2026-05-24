# Implementation Plan: 약정 체결 요청 API (SOFIT-36)

## Overview

대출 심사 승인 후 약정 체결 API를 구현합니다. 기존 프로젝트 패턴(Service Interface + Impl, Converter, ControllerDocs)을 따르며, 엔티티 메서드 추가 → 에러/성공 코드 추가 → DTO 생성 → Converter 추가 → Service 구현 → Controller 엔드포인트 추가 순서로 진행합니다.

## Tasks

- [x] 1. LoanApplication 엔티티에 contract() 메서드 추가
  - `sofit-common/.../entity/loan/LoanApplication.java`에 contract(Long requestedAmount) 비즈니스 메서드 추가
  - this.requestedAmount = requestedAmount, this.status = ApplicationStatus.CONTRACTED 설정
  - _Requirements: 2.1, 2.2_

- [x] 2. 에러 코드 및 성공 코드 추가
  - [x] 2.1 LoanErrorCode에 INVALID_STATUS, AMOUNT_EXCEEDS_APPROVED 추가
    - `sofit-user/.../exception/LoanErrorCode.java`에 enum 값 추가
    - INVALID_STATUS(HttpStatus.BAD_REQUEST, "LOAN4002", "약정 체결이 불가능한 상태입니다.")
    - AMOUNT_EXCEEDS_APPROVED(HttpStatus.BAD_REQUEST, "LOAN4003", "신청 금액이 승인 금액을 초과합니다.")
    - _Requirements: 3.2, 4.2_

  - [x] 2.2 LoanSuccessCode에 LOAN_CONTRACT_OK 추가
    - `sofit-user/.../exception/LoanSuccessCode.java`에 enum 값 추가
    - LOAN_CONTRACT_OK(HttpStatus.OK, "LOAN2013", "약정 체결이 완료되었습니다.")
    - _Requirements: 1.2_

- [x] 3. DTO 생성
  - [x] 3.1 ContractRequest 클래스 생성
    - `sofit-user/.../dto/request/ContractRequest.java` 생성
    - class 타입, @NotNull Long requestedAmount 필드, @Getter, @NoArgsConstructor
    - _Requirements: 1.3_

  - [x] 3.2 ContractResponse 레코드 생성
    - `sofit-user/.../dto/response/ContractResponse.java` 생성
    - record 타입, Long applicationId, String status 필드
    - _Requirements: 1.2_

- [x] 4. Converter에 toContractResponse() 메서드 추가
  - `sofit-user/.../converter/LoanApplicationConverter.java`에 static 메서드 추가
  - LoanApplication → ContractResponse 변환
  - _Requirements: 1.2_

- [x] 5. Service 레이어 구현
  - [x] 5.1 LoanApplicationService 인터페이스에 contractLoan() 메서드 선언 추가
    - `sofit-user/.../service/LoanApplicationService.java`에 메서드 시그니처 추가
    - ContractResponse contractLoan(Long userId, Long applicationId, ContractRequest request)
    - _Requirements: 1.1_

  - [x] 5.2 LoanApplicationServiceImpl에 contractLoan() 구현
    - `sofit-user/.../service/LoanApplicationServiceImpl.java`에 구현 추가
    - LoanDecisionRepository 의존성 주입 추가
    - 비즈니스 로직: 신청 조회 → 상태 검증 → 심사 결정 조회 → 금액 검증 → contract() 호출
    - @Transactional 어노테이션 (Dirty Checking으로 자동 저장)
    - _Requirements: 1.1, 3.1, 3.2, 4.1, 4.2, 5.1, 5.2, 6.1, 6.2_

- [x] 6. Controller 레이어 구현
  - [x] 6.1 LoanApplicationControllerDocs에 contractLoan() Swagger 문서 추가
    - `sofit-user/.../controller/LoanApplicationControllerDocs.java`에 메서드 시그니처 + Swagger 어노테이션 추가
    - @Operation, @ApiResponses (200, 400, 404)
    - _Requirements: 1.1_

  - [x] 6.2 LoanApplicationController에 POST 엔드포인트 추가
    - `sofit-user/.../controller/LoanApplicationController.java`에 @PostMapping 메서드 추가
    - POST /loan-applications/{applicationId}/contract
    - SecurityUtil.getCurrentUserId() → service.contractLoan() → ApiResponse.onSuccess()
    - _Requirements: 1.1, 1.2_

- [x] 7. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 8. 단위 테스트 작성
  - [ ]* 8.1 LoanApplication.contract() 메서드 단위 테스트
    - contract() 호출 후 status == CONTRACTED, requestedAmount 설정 확인
    - _Requirements: 2.1, 2.2_

  - [ ]* 8.2 LoanApplicationServiceImpl.contractLoan() 단위 테스트
    - 정상 케이스, APPLICATION_NOT_FOUND, INVALID_STATUS, LOAN_DECISION_NOT_FOUND, AMOUNT_EXCEEDS_APPROVED 각 시나리오
    - **Property 1: 금액 범위 검증 일관성**
    - **Property 2: 금액 초과 거부 일관성**
    - **Property 3: 상태 전이 정확성**
    - **Property 4: 비승인 상태 거부 일관성**
    - **Validates: Requirements 3.1, 3.2, 4.2, 5.2, 6.2**

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 LoanApplicationRepository.findByApplicationIdAndUser_UserId()와 LoanDecisionRepository.findByApplication_ApplicationId()를 재사용
- Dirty Checking 패턴으로 별도 save() 호출 불필요
- 프로젝트 컨벤션에 따라 Request DTO는 class, Response DTO는 record 사용
