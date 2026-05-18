# Implementation Plan: 코데프 1원 이체 계좌 인증

## Overview

코데프(CODEF) API를 활용한 1원 이체 방식의 계좌 인증 기능을 기존 LoanExecutionController/Service에 통합 구현한다. Redis 기반 인증코드 저장(TTL 300초), Rate Limiter(일일 5회), 계좌번호 마스킹 유틸리티를 포함하며, jqwik을 활용한 Property-Based 테스트로 정확성을 검증한다.

## Tasks

- [ ] 1. 에러/성공 코드 정의 및 테스트 의존성 추가
  - [ ] 1.1 LoanErrorCode에 계좌 인증 에러 코드 추가
    - `ACCOUNT4001`(400, 유효하지 않은 계좌번호), `ACCOUNT4002`(429, 일일 요청 한도 초과), `ACCOUNT4003`(400, 인증번호 불일치), `ACCOUNT4004`(400, 인증 시간 만료), `ACCOUNT4005`(400, 유효하지 않은 은행코드), `ACCOUNT5001`(502, 계좌 인증 서비스 오류) 추가
    - _Requirements: 8.2, 8.3_

  - [ ] 1.2 LoanSuccessCode에 계좌 인증 성공 코드 추가
    - `ACCOUNT_VERIFICATION_OK`(200, 1원 송금 요청 성공), `ACCOUNT_VERIFICATION_CONFIRM_OK`(200, 계좌 인증 성공) 추가
    - _Requirements: 8.1_

  - [ ] 1.3 build.gradle에 Redis, jqwik, WireMock 의존성 추가
    - `spring-boot-starter-data-redis`, `net.jqwik:jqwik:1.9.2`, `org.wiremock:wiremock-standalone:3.10.0`, `com.github.codemonstur:embedded-redis:1.4.3` 추가
    - _Requirements: 2.1, 3.1_

- [ ] 2. 계좌번호 마스킹 유틸리티 구현
  - [ ] 2.1 AccountMaskingUtil 클래스 생성
    - `sofit-user/.../domain/loan/util/AccountMaskingUtil.java` 생성
    - `mask(String accountNumber)` 정적 메서드 구현: 9자리 이상 계좌번호를 "{앞4자리}-****-{9번째부터 끝}" 형식으로 마스킹
    - 9자리 미만 입력 시 `BaseException(ACCOUNT4001)` throw
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ]* 2.2 AccountMaskingUtil Property 테스트 작성
    - **Property 1: 계좌번호 마스킹 형식 보존**
    - **Validates: Requirements 7.1, 7.2**
    - jqwik으로 9~20자리 숫자 문자열을 생성하여 마스킹 결과가 "{앞4자리}-****-{9번째부터}" 형식인지 검증

  - [ ]* 2.3 AccountMaskingUtil 단위 테스트 작성
    - 경계값 테스트 (9자리, 20자리), 8자리 미만 거부 테스트
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] 3. Request/Response DTO 생성
  - [ ] 3.1 AccountVerificationRequest DTO 생성
    - `sofit-user/.../domain/loan/dto/request/AccountVerificationRequest.java` 생성
    - `bankCode`(@NotBlank), `accountNumber`(@NotBlank, @Pattern 숫자 7~20자리) 필드 정의
    - _Requirements: 1.1, 4.1, 4.2, 4.3_

  - [ ] 3.2 AccountVerificationConfirmRequest DTO 생성
    - `sofit-user/.../domain/loan/dto/request/AccountVerificationConfirmRequest.java` 생성
    - `verificationCode`(@NotBlank, @Pattern 숫자 4자리) 필드 정의
    - _Requirements: 5.1, 4.4_

  - [ ] 3.3 AccountVerificationResponse DTO 생성
    - `sofit-user/.../domain/loan/dto/response/AccountVerificationResponse.java` 생성 (record)
    - `bankName`, `maskedAccountNumber`, `accountHolder`, `expiredAt` 필드
    - _Requirements: 1.2, 1.4, 7.4_

  - [ ] 3.4 AccountVerificationConfirmResponse DTO 생성
    - `sofit-user/.../domain/loan/dto/response/AccountVerificationConfirmResponse.java` 생성 (record)
    - `accountVerified` 필드
    - _Requirements: 5.2_

- [ ] 4. Checkpoint - 기본 구조 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. CodefClient 구현
  - [ ] 5.1 CodefClient 클래스 생성
    - `sofit-user/.../domain/loan/client/CodefClient.java` 생성
    - RestClient를 사용하여 코데프 데모 서버(`https://development.codef.io/v1/kr/bank/a/account/transfer-authentication`)로 POST 요청
    - 요청 Body: `organization`, `account`, `inPrintType("0")`, `inPrintContent("")`
    - 연결/읽기 타임아웃 30초 설정
    - 응답에서 `data.authCode` 추출하여 반환
    - 실패 시(타임아웃, 4xx/5xx) 재시도 없이 `BaseException(ACCOUNT5001)` throw
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ]* 5.2 CodefClient 단위 테스트 작성
    - MockRestServiceServer 또는 WireMock으로 성공/실패/타임아웃 시나리오 테스트
    - _Requirements: 2.4, 2.5_

- [ ] 6. Redis 기반 Rate Limiter 구현
  - [ ] 6.1 AccountVerificationRateLimiter 클래스 생성
    - `sofit-user/.../domain/loan/service/AccountVerificationRateLimiter.java` 생성
    - Redis key: `account:rate:{accountNumber}:{yyyyMMdd}` (KST 기준)
    - `isAllowed(String accountNumber)`: 현재 카운터 < 5 여부 반환
    - `increment(String accountNumber)`: INCR + TTL(자정까지 남은 초) 설정
    - Redis 장애 시 `BaseException(ACCOUNT5001)` throw (fail-closed)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 6.2 AccountVerificationRateLimiter Property 테스트 작성
    - **Property 5: Rate Limiter 허용/거부 불변식**
    - **Validates: Requirements 1.7, 3.1, 3.2**
    - jqwik으로 카운터 값(0~10)을 생성하여 5 미만이면 허용, 5 이상이면 거부 검증

  - [ ]* 6.3 AccountVerificationRateLimiter Property 테스트 작성 (카운터 정확성)
    - **Property 6: Rate Limiter 카운터 정확성**
    - **Validates: Requirements 3.3, 3.4**
    - 성공 시 n+1, 실패 시 n 유지 검증

- [ ] 7. LoanExecutionService 계좌 인증 비즈니스 로직 구현
  - [ ] 7.1 LoanExecutionService 인터페이스에 메서드 추가
    - `requestAccountVerification(Long applicationId, AccountVerificationRequest request)` 추가
    - `confirmAccountVerification(Long applicationId, AccountVerificationConfirmRequest request)` 추가
    - _Requirements: 1.1, 5.1_

  - [ ] 7.2 LoanExecutionServiceImpl에 1원 송금 요청 로직 구현
    - 은행코드 유효성 검증 (등록된 기관코드 목록 확인)
    - Rate Limiter 확인 → CodefClient 호출 → Redis Hash 저장(authCode, bankCode, accountNumber, TTL 300초) → Rate Limiter 증가
    - 마스킹된 계좌번호, bankName, accountHolder, expiredAt(현재+5분, ISO 8601) 응답 생성
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 3.1, 3.3, 4.5, 6.1, 7.4, 9.1, 9.3_

  - [ ] 7.3 LoanExecutionServiceImpl에 인증코드 확인 로직 구현
    - Redis에서 applicationId로 Hash 조회 → authCode 비교 → 성공 시 Redis 삭제 + DB 저장
    - TTL 만료(키 없음) → ACCOUNT4004, 불일치 → ACCOUNT4003
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 6.2, 6.3, 6.4, 9.4_

  - [ ]* 7.4 LoanExecutionServiceImpl 단위 테스트 작성
    - Mock: CodefClient, RedisTemplate, LoanExecutionRepository, RateLimiter
    - 정상 흐름, 은행코드 오류, Rate Limit 초과, 코데프 실패, 인증 성공/실패/만료 시나리오
    - _Requirements: 1.1~1.7, 5.1~5.7_

- [ ] 8. Checkpoint - 비즈니스 로직 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Controller 및 Swagger 문서 통합
  - [ ] 9.1 LoanExecutionControllerDocs에 계좌 인증 API 문서 추가
    - `requestAccountVerification`, `confirmAccountVerification` 메서드에 @Operation, @ApiResponse 어노테이션 정의
    - _Requirements: 8.1, 8.2_

  - [ ] 9.2 LoanExecutionController에 계좌 인증 엔드포인트 추가
    - `POST /{applicationId}/account-verification` → `requestAccountVerification`
    - `POST /{applicationId}/account-verification/confirm` → `confirmAccountVerification`
    - @Valid @RequestBody 적용, TEMP_USER_ID 패턴 유지
    - _Requirements: 1.1, 5.1, 8.4, 9.1, 9.2_

  - [ ] 9.3 SecurityConfig에 계좌 인증 엔드포인트 permitAll 추가
    - `/api/loan-applications/*/account-verification/**` 경로 허용
    - _Requirements: 9.1_

- [ ] 10. Property-Based 테스트 작성 (입력값 검증)
  - [ ]* 10.1 계좌번호 검증 Property 테스트 작성
    - **Property 2: 유효하지 않은 계좌번호 거부**
    - **Validates: Requirements 1.5, 4.3**
    - jqwik으로 비숫자 문자 포함, 7자리 미만, 20자리 초과 문자열 생성하여 검증 실패 확인

  - [ ]* 10.2 인증번호 검증 Property 테스트 작성
    - **Property 3: 유효하지 않은 인증번호 거부**
    - **Validates: Requirements 4.4, 5.7**
    - jqwik으로 4자리 숫자가 아닌 문자열 생성하여 검증 실패 확인

  - [ ]* 10.3 은행코드 검증 Property 테스트 작성
    - **Property 4: 유효하지 않은 은행코드 거부**
    - **Validates: Requirements 4.5**
    - jqwik으로 등록되지 않은 은행코드 문자열 생성하여 검증 실패 확인

- [ ] 11. Property-Based 테스트 작성 (인증 로직)
  - [ ]* 11.1 인증코드 일치 판정 Property 테스트 작성
    - **Property 7: 인증코드 일치 판정 정확성**
    - **Validates: Requirements 5.2, 5.5**
    - jqwik으로 4자리 숫자 쌍 생성하여 동일하면 성공, 다르면 실패 검증

  - [ ]* 11.2 인증 성공 후 재사용 방지 Property 테스트 작성
    - **Property 8: 인증 성공 후 재사용 방지**
    - **Validates: Requirements 6.3**
    - 인증 성공 후 동일 applicationId로 재시도 시 ACCOUNT4004 에러 검증

  - [ ]* 11.3 응답에 원본 계좌번호 미포함 Property 테스트 작성
    - **Property 9: 응답에 원본 계좌번호 미포함**
    - **Validates: Requirements 7.4**
    - jqwik으로 9~20자리 계좌번호 생성 후 응답 JSON 직렬화 결과에 원본 미포함 확인

- [ ] 12. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능
- Checkpoint에서 점진적 검증 수행
- Property 테스트는 jqwik 라이브러리 사용, `@Tag("Feature: codef-account-verification, Property N: ...")` 태그 부착
- 기존 LoanExecutionController/Service에 통합하므로 별도 Controller 생성 불필요
- Redis 의존성은 sofit-user build.gradle에 추가
- 코데프 API 호출은 RestClient 사용 (Spring Boot 기본 제공)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "3.1", "3.2", "3.3", "3.4"] },
    { "id": 2, "tasks": ["2.2", "2.3", "5.1", "6.1"] },
    { "id": 3, "tasks": ["5.2", "6.2", "6.3", "7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3"] },
    { "id": 5, "tasks": ["7.4", "9.1"] },
    { "id": 6, "tasks": ["9.2", "9.3"] },
    { "id": 7, "tasks": ["10.1", "10.2", "10.3"] },
    { "id": 8, "tasks": ["11.1", "11.2", "11.3"] }
  ]
}
```
