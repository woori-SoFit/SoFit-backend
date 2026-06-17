# Implementation Plan: 대출 신청 단계별 래퍼 API (Step 2~6)

## Overview

대출 신청 플로우의 중간 단계(Step 2~6)를 처리하는 래퍼 API를 구현한다. LoanStepController → LoanStepService → 범용 서비스(ConsentService, AuthService, BizInfoService, MybizService) 호출 + lastCompletedStep 업데이트 구조로 구현하며, 기존 프로젝트의 Service interface + ServiceImpl, ControllerDocs, Converter 패턴을 따른다.

## Tasks

- [ ] 1. Request/Response DTO 및 에러/성공 코드 정의
  - [ ] 1.1 Request DTO 생성 (ConsentStepRequest, AuthStepRequest, BizInfoStepRequest, MydataStepRequest, MybizStepRequest)
    - `sofit-user/.../loan/dto/request/` 하위에 각 Request class 생성
    - Bean Validation 어노테이션 적용 (@NotEmpty, @NotBlank, @Pattern)
    - ConsentItem 내부 클래스 또는 별도 DTO 정의
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1_

  - [ ] 1.2 Response DTO 생성 (LoanStepResponse, BizInfoStepResponse)
    - `sofit-user/.../loan/dto/response/` 하위에 record로 생성
    - LoanStepResponse: applicationId, completedStep
    - BizInfoStepResponse: applicationId, completedStep, BizInfo(상호명, 대표자명, 업종, 개업일)
    - _Requirements: 1.3, 2.3, 3.3, 4.4, 5.3_

  - [ ] 1.3 LoanErrorCode에 새 에러 코드 추가
    - STEP_ORDER_VIOLATION, REQUIRED_CONSENT_MISSING, INVALID_PIN_FORMAT, INVALID_BIZ_NO_FORMAT, PIN_AUTH_FAILED, APPLICATION_NOT_OWNED, BIZ_INFO_NOT_FOUND, EXTERNAL_SERVICE_ERROR, MYBIZ_SERVICE_ERROR
    - _Requirements: 1.4~1.8, 2.4~2.8, 3.4~3.8, 4.5~4.9, 5.4~5.8, 6.4, 6.8_

  - [ ] 1.4 LoanSuccessCode에 새 성공 코드 추가
    - LOAN_STEP_CONSENT_OK, LOAN_STEP_AUTH_OK, LOAN_STEP_BIZ_INFO_OK, LOAN_STEP_MYDATA_OK, LOAN_STEP_MYBIZ_OK
    - _Requirements: 1.3, 2.3, 3.3, 4.4, 5.3_

- [ ] 2. 범용 서비스 인터페이스 및 Stub 구현체 생성
  - [ ] 2.1 ConsentService 인터페이스 및 ConsentServiceImpl stub 생성
    - `sofit-user/.../loan/service/` 하위에 생성
    - 약관 동의 처리 메서드 정의 (consentItems를 받아 처리)
    - Stub 구현체는 성공 반환 (추후 실제 로직 구현)
    - _Requirements: 1.1, 4.1_

  - [ ] 2.2 AuthService 인터페이스 및 AuthServiceImpl stub 생성
    - PIN 인증 메서드 정의 (pin을 받아 인증 처리)
    - Stub 구현체는 성공 반환
    - _Requirements: 2.1_

  - [ ] 2.3 BizInfoService 인터페이스 및 BizInfoServiceImpl stub 생성
    - 사업자 정보 조회 메서드 정의 (bizNo를 받아 BizInfoResult 반환)
    - BizInfoResult DTO 정의 (businessName, representativeName, businessCategory, openDate)
    - Stub 구현체는 Mock 데이터 반환
    - _Requirements: 3.1_

  - [ ] 2.4 MybizService 인터페이스 및 MybizServiceImpl stub 생성
    - 마이비즈데이터 수집 메서드 정의 (bizNo를 받아 처리)
    - Stub 구현체는 성공 반환
    - _Requirements: 5.1_

- [ ] 3. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. LoanStepService 구현
  - [ ] 4.1 LoanStepService 인터페이스 정의
    - processConsent, processAuth, processBizInfo, processMydata, processMybiz 메서드 선언
    - 각 메서드는 userId, applicationId, 해당 Request DTO를 파라미터로 받음
    - _Requirements: 6.7_

  - [ ] 4.2 LoanStepServiceImpl 구현 - 공통 검증 로직
    - validateAndGetApplication(userId, applicationId, requiredStep) private 메서드 구현
    - 검증 순서: 존재 확인(404) → 본인 소유(403) → DRAFT 상태(400) → 단계 순서(400)
    - @Transactional 적용
    - _Requirements: 6.1, 6.4, 6.8, 1.4, 1.5, 1.7_

  - [ ] 4.3 LoanStepServiceImpl 구현 - processConsent (Step 2)
    - 공통 검증 (requiredStep = null) → ConsentService 호출 → lastCompletedStep = CONSENT_DONE
    - _Requirements: 1.1, 1.2, 1.6, 1.8_

  - [ ] 4.4 LoanStepServiceImpl 구현 - processAuth (Step 3)
    - 공통 검증 (requiredStep = CONSENT_DONE) → AuthService 호출 → lastCompletedStep = AUTH_DONE
    - _Requirements: 2.1, 2.2, 2.7_

  - [ ] 4.5 LoanStepServiceImpl 구현 - processBizInfo (Step 4)
    - 공통 검증 (requiredStep = AUTH_DONE) → BizInfoService 호출 → lastCompletedStep = BIZ_INFO_DONE
    - BizInfoStepResponse 반환 (사업자 정보 포함)
    - _Requirements: 3.1, 3.2, 3.3, 3.8_

  - [ ] 4.6 LoanStepServiceImpl 구현 - processMydata (Step 5)
    - 공통 검증 (requiredStep = BIZ_INFO_DONE) → ConsentService 호출 → lastCompletedStep = DATA_COLLECTED
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ] 4.7 LoanStepServiceImpl 구현 - processMybiz (Step 6)
    - 공통 검증 (requiredStep = DATA_COLLECTED) → MybizService 호출 → lastCompletedStep = MYBIZ_CONNECTED
    - _Requirements: 5.1, 5.2, 5.8_

  - [ ]* 4.8 LoanStepServiceImpl 단위 테스트 작성
    - 각 단계별 정상 처리 시나리오 (범용 서비스 mock)
    - 공통 검증 로직 실패 시나리오 (404, 403, 400)
    - 범용 서비스 호출 실패 시 예외 전파 확인
    - _Requirements: 1.1~1.8, 2.1~2.8, 3.1~3.8, 4.1~4.9, 5.1~5.8, 6.1, 6.4, 6.8_

- [ ] 5. Converter 및 Controller 구현
  - [ ] 5.1 LoanStepConverter 생성
    - toStepResponse(LoanApplication) → LoanStepResponse 변환
    - toBizInfoStepResponse(LoanApplication, BizInfoResult) → BizInfoStepResponse 변환
    - _Requirements: 6.6_

  - [ ] 5.2 LoanStepControllerDocs 인터페이스 생성
    - 5개 엔드포인트에 대한 Swagger @Operation, @ApiResponse 어노테이션 정의
    - _Requirements: 6.5_

  - [ ] 5.3 LoanStepController 구현
    - LoanStepControllerDocs implements
    - @RestController, @RequestMapping("/api/loan-applications/{applicationId}/steps")
    - SecurityUtil.getCurrentUserId()로 userId 추출
    - 각 엔드포인트에서 LoanStepService 호출 후 ApiResponse.onSuccess() 반환
    - _Requirements: 6.2, 6.3, 6.5_

  - [ ]* 5.4 LoanStepController MockMvc 테스트 작성
    - 각 엔드포인트 정상 응답 확인
    - Bean Validation 실패 시 400 응답 확인
    - _Requirements: 1.6, 2.6, 3.6, 4.8, 5.6_

- [ ] 6. Checkpoint - 전체 컴파일 및 기존 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Property-Based 테스트 작성 (jqwik)
  - [ ]* 7.1 Property 1 테스트: 단계 완료 시 올바른 lastCompletedStep 업데이트
    - **Property 1: 단계 완료 시 올바른 lastCompletedStep 업데이트**
    - 유효한 DRAFT 상태 + 올바른 이전 단계 완료 상태에서 서비스 호출 성공 시 정확한 step 업데이트 검증
    - **Validates: Requirements 1.2, 2.2, 3.2, 4.3, 5.2**

  - [ ]* 7.2 Property 2 테스트: 단계 순서 위반 시 거부
    - **Property 2: 단계 순서 위반 시 거부**
    - 요구되는 이전 lastCompletedStep과 실제 값이 불일치 시 400 에러 + 상태 미변경 검증
    - Generator: LastCompletedStep enum에서 올바르지 않은 값 생성
    - **Validates: Requirements 1.7, 2.8, 3.7, 4.7, 5.7, 6.8**

  - [ ]* 7.3 Property 3 테스트: DRAFT 상태가 아닌 신청에 대한 거부
    - **Property 3: DRAFT 상태가 아닌 신청에 대한 거부**
    - SUBMITTED, APPROVED, REJECTED 등 DRAFT가 아닌 상태에서 모든 단계 요청 시 400 에러 검증
    - Generator: ApplicationStatus enum에서 DRAFT 제외한 값 생성
    - **Validates: Requirements 1.5, 2.5, 3.5, 4.6, 5.5**

  - [ ]* 7.4 Property 5 테스트: 유효하지 않은 입력 형식 거부
    - **Property 5: 유효하지 않은 입력 형식 거부**
    - 6자리 숫자가 아닌 PIN, 10자리 숫자가 아닌 bizNo, 빈 동의 목록에 대해 400 에러 + 상태 미변경 검증
    - Generator: 잘못된 형식의 문자열 생성
    - **Validates: Requirements 1.6, 2.6, 3.6, 4.8, 5.6**

  - [ ]* 7.5 Property 6 테스트: 서비스 실패 시 트랜잭션 롤백
    - **Property 6: 서비스 실패 시 트랜잭션 롤백 (상태 미변경)**
    - 범용 서비스 호출 실패 시 lastCompletedStep이 호출 이전 상태 유지 검증
    - **Validates: Requirements 1.8, 6.1**

  - [ ]* 7.6 Property 7 테스트: 본인 소유 확인 실패 시 403 반환
    - **Property 7: 본인 소유 확인 실패 시 403 반환**
    - userId와 application 소유자 불일치 시 403 에러 + 단계 처리 미수행 검증
    - **Validates: Requirements 6.4**

- [ ] 8. SecurityConfig 업데이트 및 최종 통합
  - [ ] 8.1 SecurityConfig에 새 엔드포인트 권한 설정 추가
    - `/api/loan-applications/*/steps/**` 경로에 대해 인증된 USER 역할 허용
    - _Requirements: 6.3, 6.4_

  - [ ] 8.2 전체 통합 확인 및 누락 사항 점검
    - 모든 컴포넌트 간 의존성 연결 확인
    - 기존 LoanApplicationController와의 URL 충돌 없음 확인
    - _Requirements: 6.1~6.8_

- [ ] 9. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 범용 서비스(ConsentService, AuthService, BizInfoService, MybizService)는 stub으로 구현하며, 실제 외부 연동은 별도 이슈에서 처리
- 각 task는 기존 프로젝트 패턴(Service interface + ServiceImpl, ControllerDocs, Converter, record Response)을 따름
- Property-based 테스트는 jqwik 라이브러리 사용, 최소 100회 반복 실행
- 에러 코드는 기존 LoanErrorCode enum에 추가하여 일관성 유지

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4"] },
    { "id": 2, "tasks": ["4.1", "5.1"] },
    { "id": 3, "tasks": ["4.2"] },
    { "id": 4, "tasks": ["4.3", "4.4", "4.5", "4.6", "4.7"] },
    { "id": 5, "tasks": ["4.8", "5.2"] },
    { "id": 6, "tasks": ["5.3"] },
    { "id": 7, "tasks": ["5.4", "8.1"] },
    { "id": 8, "tasks": ["8.2"] },
    { "id": 9, "tasks": ["7.1", "7.2", "7.3", "7.4", "7.5", "7.6"] }
  ]
}
```
