# Implementation Plan: 회원 정보 조회 (User Profile Inquiry)

## Overview

로그인한 사용자가 `GET /api/users/me` 엔드포인트를 통해 자신의 프로필 정보(이름, 아이디, 전화번호, 주민번호)를 조회하는 API를 구현한다. 세션 기반 인증, 주민번호 마스킹, 전화번호 포맷팅, Swagger 문서화를 포함한다.

## Tasks

- [x] 1. Response DTO 및 코드 정의
  - [x] 1.1 UserProfileResponse record 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/user/dto/response/UserProfileResponse.java` 생성
    - 필드: `name`, `username`, `phoneNumber`, `residentNumber` (모두 String)
    - record 타입으로 작성
    - _Requirements: 1.1, 1.2_

  - [x] 1.2 UserSuccessCode enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/user/exception/UserSuccessCode.java` 생성
    - `BaseSuccessCode` 인터페이스 구현
    - `USER_PROFILE_OK(HttpStatus.OK, "MEMBER2001", "회원 정보 조회에 성공했습니다.")` 정의
    - _Requirements: 1.1_

  - [x] 1.3 UserErrorCode enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/user/exception/UserErrorCode.java` 생성
    - `BaseErrorCode` 인터페이스 구현
    - `INACTIVE_USER(HttpStatus.FORBIDDEN, "AUTH4031", "탈퇴한 계정입니다.")` 정의
    - _Requirements: 4.3_

- [x] 2. UserConverter 구현
  - [x] 2.1 UserConverter 클래스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/user/converter/UserConverter.java` 생성
    - `maskResidentNumber(String residentNumber)` 메서드 구현
      - null/빈 문자열 → 빈 문자열 반환
      - 1~6자리 → 원본 + "-" + "******" 반환
      - 7자리 이상 → 앞 6자리 + "-" + 7번째 자리 + "******" 형식 (총 14자)
    - `formatPhoneNumber(String phoneNumber)` 메서드 구현
      - null/빈 문자열 → 빈 문자열 반환
      - 11자리 → `NNN-NNNN-NNNN` 형식
      - 10자리 → `NN-NNNN-NNNN` 형식
      - 그 외 → 원본 그대로 반환
    - `toUserProfileResponse(User user)` 메서드 구현
      - User 엔티티의 name, loginId, phoneNumber, residentNumber를 변환하여 UserProfileResponse 생성
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 2.2 Property 1: 주민번호 마스킹 형식 보존 테스트
    - **Property 1: 주민번호 마스킹 형식 보존**
    - **Validates: Requirements 2.1, 2.2**
    - jqwik을 사용하여 7~13자리 숫자 문자열에 대해 마스킹 결과가 `^\d{6}-\d\*{6}$` 패턴을 만족하고, 앞 7자리(하이픈 제외)가 원본과 동일한지 검증
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/converter/UserConverterPropertyTest.java`

  - [ ]* 2.3 Property 2: 유효한 전화번호 포맷팅 테스트
    - **Property 2: 유효한 전화번호 포맷팅**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - jqwik을 사용하여 10자리/11자리 숫자 문자열에 대해 포맷팅 결과에서 하이픈 제거 시 원본과 동일한지(라운드트립), 형식 패턴이 올바른지 검증
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/converter/UserConverterPropertyTest.java`

  - [ ]* 2.4 Property 3: 유효하지 않은 길이의 전화번호 원본 보존 테스트
    - **Property 3: 유효하지 않은 길이의 전화번호 원본 보존**
    - **Validates: Requirements 3.5**
    - jqwik을 사용하여 10/11자리가 아닌 문자열에 대해 포맷팅 결과가 원본과 동일한지 검증
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/converter/UserConverterPropertyTest.java`

  - [ ]* 2.5 UserConverter 단위 테스트 작성
    - 주민번호 마스킹: 정상(7자리), null, 빈 문자열, 1~6자리 엣지 케이스
    - 전화번호 포맷팅: 11자리, 10자리, null, 빈 문자열, 비정상 길이(9자리, 12자리)
    - toUserProfileResponse 변환 정합성 확인
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/converter/UserConverterTest.java`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3. Checkpoint - Converter 구현 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. UserService 구현
  - [x] 4.1 UserService 인터페이스 및 UserServiceImpl 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/user/service/UserService.java` 인터페이스 생성
      - `UserProfileResponse findUser(HttpSession session)` 메서드 선언
    - `sofit-user/src/main/java/com/sofit/user/domain/user/service/UserServiceImpl.java` 구현체 생성
      - 세션에서 `userId` 속성 추출
      - `UserRepository.findById(userId)`로 사용자 조회
      - 사용자 미존재 시 `BaseException(GeneralErrorCode.NOT_FOUND)` throw
      - 사용자 상태가 INACTIVE이면 `BaseException(UserErrorCode.INACTIVE_USER)` throw
      - `UserConverter.toUserProfileResponse(user)` 호출하여 응답 반환
    - _Requirements: 1.1, 1.2, 4.2, 4.3_

  - [ ]* 4.2 UserServiceImpl 단위 테스트 작성
    - Mockito로 UserRepository, UserConverter mock 처리
    - 정상 조회 (ACTIVE 사용자) 시나리오
    - 사용자 미존재 시 BaseException(NOT_FOUND) 발생 확인
    - INACTIVE 사용자 시 BaseException(INACTIVE_USER) 발생 확인
    - 세션에서 userId 추출 로직 검증
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/service/UserServiceImplTest.java`
    - _Requirements: 4.2, 4.3_

- [x] 5. Controller 및 Swagger 문서화
  - [x] 5.1 UserControllerDocs 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/user/controller/UserControllerDocs.java` 생성
    - `@Tag(name = "회원", description = "회원 정보 관련 API")` 어노테이션
    - `@Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")` 어노테이션
    - `@ApiResponses`에 200(조회 성공), 401(인증 실패), 403(탈퇴 계정), 404(사용자 미존재) 응답 코드 명시
    - 기존 AuthControllerDocs 패턴과 동일한 구조 사용
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 5.2 UserController 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/user/controller/UserController.java` 생성
    - `UserControllerDocs` implements
    - `@RestController`, `@RequestMapping("/api/users")`, `@RequiredArgsConstructor`
    - `@GetMapping("/me")` 엔드포인트 구현
    - `HttpSession` 파라미터로 세션 수신
    - `userService.findUser(session)` 호출 후 `ApiResponse.onSuccess(UserSuccessCode.USER_PROFILE_OK, response)` 반환
    - _Requirements: 1.1, 1.2_

  - [ ]* 5.3 UserController MockMvc 테스트 작성
    - MockMvc를 사용한 `GET /api/users/me` 엔드포인트 테스트
    - 성공 응답 시 HTTP 200, 응답 코드 `MEMBER2001`, 메시지 확인
    - 응답 body에 name, username, phoneNumber, residentNumber 필드 존재 확인
    - 테스트 위치: `sofit-user/src/test/java/com/sofit/user/domain/user/controller/UserControllerTest.java`
    - _Requirements: 1.1, 1.2, 5.1_

- [x] 6. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능
- Checkpoints에서 점진적 검증 수행
- Property 테스트는 jqwik 라이브러리를 사용하여 Correctness Properties를 검증
- 단위 테스트는 JUnit 5 + Mockito로 특정 예제와 엣지 케이스를 검증
- User 엔티티와 UserRepository는 sofit-common 모듈에 이미 존재
- 세션 인증 필터(SessionValidationFilter)는 기존 global/filter에 구현되어 있으므로 별도 구현 불필요

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2", "5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3"] }
  ]
}
```
