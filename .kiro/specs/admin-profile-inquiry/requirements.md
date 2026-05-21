# Requirements Document

## Introduction

로그인한 관리자(은행원)가 자신의 프로필 정보를 조회하는 API를 제공한다. `GET /api/admin/auth/me` 엔드포인트를 통해 이름, 로그인 아이디, 전화번호, 역할을 반환한다. 세션 기반 인증을 사용하며, 인증 실패 시 JSON 형식의 에러 응답을 반환한다.

## Glossary

- **Admin_Profile_API**: 로그인한 관리자의 프로필 정보를 조회하는 REST API 엔드포인트 (`GET /api/admin/auth/me`)
- **Session**: Redis에 저장되는 관리자 인증 세션. 로그인 시 생성되며 관리자 식별에 사용
- **Phone_Number_Formatting**: 하이픈 없이 저장된 전화번호를 하이픈 포함 형태로 변환하는 처리 (예: `01012345678` → `010-1234-5678`)
- **AdminAuthConverter**: Entity를 Response DTO로 변환하는 역할을 담당하는 클래스
- **AdminMeResponse**: 관리자 프로필 정보를 담는 응답 DTO (record 타입)
- **CustomAuthenticationEntryPoint**: Spring Security에서 인증 실패 시 JSON 형식의 에러 응답을 반환하는 컴포넌트

## Requirements

### Requirement 1: 관리자 정보 조회

**User Story:** 로그인한 관리자(은행원)로서, 내 프로필 정보를 조회하고 싶다. 그래서 관리자 페이지에서 내 이름, 아이디, 연락처, 역할을 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 관리자가 `GET /api/admin/auth/me`를 요청하면, THE Admin_Profile_API SHALL HTTP 200 상태 코드와 함께 응답 코드 `ADMIN2001`, 메시지 `관리자 정보 조회에 성공했습니다.`를 반환한다.
2. WHEN 인증된 관리자가 `GET /api/admin/auth/me`를 요청하면, THE Admin_Profile_API SHALL `result` 필드에 이름(name), 로그인 아이디(loginId), 포맷팅된 전화번호(phoneNumber, 하이픈 포함 형태), 역할(role)을 포함하여 반환한다.

### Requirement 2: 전화번호 포맷팅

**User Story:** 관리자로서, 전화번호가 읽기 쉬운 형태로 표시되길 원한다. 그래서 내 연락처를 쉽게 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 관리자 정보를 조회하면, THE AdminAuthConverter SHALL 전화번호를 하이픈 포함 형태로 변환하여 반환한다.
2. THE AdminAuthConverter SHALL 11자리 전화번호를 `NNN-NNNN-NNNN` 형식으로 변환한다 (예: `01012345678` → `010-1234-5678`).
3. THE AdminAuthConverter SHALL 10자리 전화번호를 `NN-NNNN-NNNN` 형식으로 변환한다 (예: `0212345678` → `02-1234-5678`).
4. IF 전화번호가 null 또는 빈 문자열이면, THEN THE AdminAuthConverter SHALL 빈 문자열("")을 반환한다.
5. IF 전화번호가 10자리 또는 11자리가 아니면, THEN THE AdminAuthConverter SHALL 포맷팅 없이 원본 문자열을 그대로 반환한다.

### Requirement 3: 인증 검증

**User Story:** 서비스 운영자로서, 인증되지 않은 사용자의 관리자 정보 조회를 차단하고 싶다. 그래서 관리자 개인정보가 노출되지 않는다.

#### Acceptance Criteria

1. IF 세션이 존재하지 않거나 만료된 상태에서 `GET /api/admin/auth/me`를 요청하면, THEN THE Admin_Profile_API SHALL HTTP 401 상태 코드와 함께 에러 코드 `AUTH4011`, 메시지 `세션이 만료되었습니다. 다시 로그인해 주세요.`, `isSuccess: false`를 포함한 JSON 응답을 반환한다.
2. IF 세션에 저장된 사용자 ID에 해당하는 사용자가 데이터베이스에 존재하지 않으면, THEN THE Admin_Profile_API SHALL HTTP 404 상태 코드와 함께 에러 코드 `AUTH4041`, 메시지 `요청한 리소스를 찾을 수 없습니다.`를 공통 응답 포맷으로 반환한다.
3. IF 사용자의 상태가 INACTIVE(탈퇴)이면, THEN THE Admin_Profile_API SHALL HTTP 404 상태 코드와 함께 에러 코드 `AUTH4041`을 반환한다 (미존재와 동일하게 처리하여 계정 존재 여부를 노출하지 않음).

### Requirement 4: 인증 실패 JSON 응답

**User Story:** 프론트엔드 개발자로서, 인증 실패 시에도 일관된 JSON 형식의 에러 응답을 받고 싶다. 그래서 에러 처리 로직을 통일할 수 있다.

#### Acceptance Criteria

1. WHEN 인증되지 않은 요청이 보호된 엔드포인트에 접근하면, THE CustomAuthenticationEntryPoint SHALL Spring Security 기본 응답 대신 `Content-Type: application/json` 헤더와 함께 공통 에러 응답 포맷의 JSON 본문을 반환한다.
2. THE CustomAuthenticationEntryPoint SHALL 응답 본문에 `isSuccess: false`, `code: "AUTH4011"`, `message: "세션이 만료되었습니다. 다시 로그인해 주세요."`, `result: null`을 포함한다.

### Requirement 5: Swagger 문서화

**User Story:** 프론트엔드 개발자로서, 관리자 정보 조회 API의 명세를 Swagger에서 확인하고 싶다. 그래서 API 연동 시 정확한 요청/응답 형식을 파악할 수 있다.

#### Acceptance Criteria

1. THE Admin_Profile_API SHALL AdminAuthControllerDocs 인터페이스에 `findMe()` 메서드의 Swagger 어노테이션(@Operation, @ApiResponses)을 정의한다.
2. THE AdminAuthControllerDocs SHALL @Operation 어노테이션에 API 요약(summary)과 상세 설명(description)을 포함하고, @ApiResponses에 성공 응답(200), 인증 실패(401), 사용자 미존재(404) 응답 코드와 각 코드별 설명을 명시한다.
