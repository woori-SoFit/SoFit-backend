# Requirements Document

## Introduction

로그인한 사용자가 자신의 프로필 정보를 조회하는 API를 제공한다. `GET /api/users/me` 엔드포인트를 통해 이름, 아이디, 전화번호, 주민번호를 반환한다. 세션 기반 인증을 사용한다.

## Glossary

- **User_Profile_API**: 로그인한 사용자의 프로필 정보를 조회하는 REST API 엔드포인트 (`GET /api/users/me`)
- **Session**: Redis에 저장되는 사용자 인증 세션. 로그인 시 생성되며 사용자 식별에 사용
- **Resident_Number_Masking**: 주민번호의 앞 7자리(생년월일 6자리 + 성별 구분 1자리)만 표시하고 나머지 6자리를 `*`로 대체하는 처리 (예: `900101-1******`)
- **Phone_Number_Formatting**: 하이픈 없이 저장된 전화번호를 하이픈 포함 형태로 변환하는 처리 (예: `01012345678` → `010-1234-5678`)
- **UserConverter**: Entity를 Response DTO로 변환하는 역할을 담당하는 클래스
- **UserProfileResponse**: 사용자 프로필 정보를 담는 응답 DTO (record 타입)

## Requirements

### Requirement 1: 회원 정보 조회

**User Story:** 로그인한 사용자로서, 내 프로필 정보를 조회하고 싶다. 그래서 마이페이지에서 내 이름, 아이디, 연락처, 주민번호를 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 `GET /api/users/me`를 요청하면, THE User_Profile_API SHALL HTTP 200 상태 코드와 함께 응답 코드 `MEMBER2001`, 메시지 `회원 정보 조회에 성공했습니다.`를 반환한다.
2. WHEN 인증된 사용자가 `GET /api/users/me`를 요청하면, THE User_Profile_API SHALL `result` 필드에 이름(name), 아이디(username), 포맷팅된 전화번호(phoneNumber, 하이픈 포함 형태), 마스킹된 주민번호(residentNumber, `NNNNNN-N******` 형태)를 포함하여 반환한다.
3. WHEN 인증된 사용자가 `GET /api/users/me`를 요청하면, THE User_Profile_API SHALL 2초 이내에 응답을 반환한다.

### Requirement 2: 주민번호 마스킹 처리

**User Story:** 서비스 운영자로서, 사용자의 주민번호가 API 응답에서 마스킹 처리되길 원한다. 그래서 개인정보 유출 위험을 최소화할 수 있다.

#### Acceptance Criteria

1. WHEN 회원 정보를 조회하면, THE UserConverter SHALL 주민번호의 앞 7자리(생년월일 6자리 + 성별 구분 1자리)만 표시하고 뒤 6자리를 `*`로 대체하여 총 14자(하이픈 포함) 길이의 마스킹된 문자열을 반환한다.
2. THE UserConverter SHALL 마스킹된 주민번호를 `NNNNNN-N******` 형식으로 반환한다 (하이픈 포함, 뒤 6자리는 `*`로 대체).
3. IF 주민번호 데이터가 null 또는 빈 문자열이면, THEN THE UserConverter SHALL 빈 문자열을 반환한다.
4. IF 주민번호 데이터가 1자리 이상 7자리 미만이면, THEN THE UserConverter SHALL 원본 데이터 뒤에 하이픈과 `*` 6개를 붙여 반환한다.

### Requirement 3: 전화번호 포맷팅

**User Story:** 사용자로서, 전화번호가 읽기 쉬운 형태로 표시되길 원한다. 그래서 마이페이지에서 내 연락처를 쉽게 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 회원 정보를 조회하면, THE UserConverter SHALL 전화번호를 하이픈 포함 형태로 변환하여 반환한다.
2. THE UserConverter SHALL 11자리 전화번호를 `NNN-NNNN-NNNN` 형식으로 변환한다 (예: `01012345678` → `010-1234-5678`).
3. THE UserConverter SHALL 10자리 전화번호를 `NN-NNNN-NNNN` 형식으로 변환한다 (예: `0212345678` → `02-1234-5678`).
4. IF 전화번호가 null 또는 빈 문자열이면, THEN THE UserConverter SHALL 빈 문자열("")을 반환한다.
5. IF 전화번호가 10자리 또는 11자리가 아니면, THEN THE UserConverter SHALL 포맷팅 없이 원본 문자열을 그대로 반환한다.

### Requirement 4: 인증 검증

**User Story:** 서비스 운영자로서, 인증되지 않은 사용자의 정보 조회를 차단하고 싶다. 그래서 타인의 개인정보가 노출되지 않는다.

#### Acceptance Criteria

1. IF 세션이 존재하지 않거나 만료된 상태에서 `GET /api/users/me`를 요청하면, THEN THE User_Profile_API SHALL HTTP 401 상태 코드와 함께 `COMMON4001` 에러 코드 및 `isSuccess: false`를 포함한 공통 응답 포맷으로 반환한다.
2. IF 세션에 저장된 사용자 ID에 해당하는 사용자가 데이터베이스에 존재하지 않으면, THEN THE User_Profile_API SHALL HTTP 404 상태 코드와 함께 `COMMON4004` 에러 코드를 공통 응답 포맷으로 반환한다.
3. IF 사용자의 상태가 INACTIVE(탈퇴)이면, THEN THE User_Profile_API SHALL HTTP 403 상태 코드와 함께 `AUTH4031` 에러 코드 및 탈퇴 계정임을 나타내는 메시지를 공통 응답 포맷으로 반환한다.

### Requirement 5: Swagger 문서화

**User Story:** 프론트엔드 개발자로서, 회원 정보 조회 API의 명세를 Swagger에서 확인하고 싶다. 그래서 API 연동 시 정확한 요청/응답 형식을 파악할 수 있다.

#### Acceptance Criteria

1. THE User_Profile_API SHALL UserControllerDocs 인터페이스에 Swagger 어노테이션(@Operation, @ApiResponses)을 정의하고, UserController가 해당 인터페이스를 implements하여 어노테이션과 비즈니스 로직을 분리한다.
2. THE UserControllerDocs SHALL @Operation 어노테이션에 API 요약(summary)과 상세 설명(description)을 포함하고, @ApiResponses에 성공 응답(200), 인증 실패(401), 접근 불가(403), 사용자 미존재(404) 응답 코드와 각 코드별 설명을 명시한다.
3. THE UserControllerDocs SHALL @Tag 어노테이션을 사용하여 API 그룹명과 그룹 설명을 명시한다.
