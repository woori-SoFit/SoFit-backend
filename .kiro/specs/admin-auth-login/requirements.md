# Requirements Document

## Introduction

SoFit 관리자 페이지(sofit-admin 모듈)의 로그인 API를 정의한다. 관리자 페이지는 은행원(ADMIN_BANK_TELLER), 지점장(ADMIN_BANK_MANAGER), 개발자(ADMIN_DEV) 역할을 가진 사용자만 접근 가능하며, 일반 고객(USER)은 접근할 수 없다. 세션 기반 인증을 사용하며 Redis를 세션 저장소로 활용한다.

## Glossary

- **Admin_Auth_System**: 관리자 페이지 인증을 담당하는 시스템 (sofit-admin 모듈의 auth 도메인)
- **Session_Store**: Redis 기반 외부 세션 저장소
- **Admin_User**: ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 역할을 가진 사용자
- **User Entity**: sofit-common 모듈에 정의된 공통 사용자 엔티티 (users 테이블)

## Requirements

### Requirement 1: 관리자 페이지 로그인

**User Story:** As a 관리자(은행원/지점장/개발자), I want 아이디와 비밀번호로 관리자 페이지에 로그인하여, so that 관리자 기능을 이용할 수 있다.

#### Acceptance Criteria

1. WHEN 아이디(loginId)와 비밀번호(password)가 `POST /api/admin/auth/login` 요청으로 전달되면, THE Admin_Auth_System SHALL 아이디로 사용자를 조회하고 저장된 BCrypt 해시와 비밀번호를 검증한다.
2. WHEN 인증이 성공하고 사용자의 role이 ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 중 하나이면, THE Admin_Auth_System SHALL Session_Store에 세션을 생성하고 세션 ID를 쿠키로 반환하며, 응답 본문에 사용자 ID(userId), 이름(name), 역할(role)을 포함한다.
3. WHEN 인증이 성공하면, THE Admin_Auth_System SHALL 세션에 사용자 ID, 역할(role), 로그인 시각을 저장한다.
4. IF 아이디에 해당하는 사용자가 존재하지 않는 경우, THEN THE Admin_Auth_System SHALL "아이디 또는 비밀번호가 올바르지 않습니다." 메시지와 함께 인증 실패 에러(AUTH4001, 400)를 반환한다.
5. IF 비밀번호가 일치하지 않는 경우, THEN THE Admin_Auth_System SHALL 사용자 미존재와 동일한 인증 실패 에러(AUTH4001, 400)를 반환한다.
6. IF 사용자의 role이 USER인 경우(관리자 권한이 없는 경우), THEN THE Admin_Auth_System SHALL "아이디 또는 비밀번호가 올바르지 않습니다." 메시지와 함께 인증 실패 에러(AUTH4001, 400)를 반환한다.
7. IF 사용자의 status가 INACTIVE인 경우, THEN THE Admin_Auth_System SHALL "아이디 또는 비밀번호가 올바르지 않습니다." 메시지와 함께 인증 실패 에러(AUTH4001, 400)를 반환한다.
8. IF 아이디 또는 비밀번호가 빈 값이거나 누락된 경우, THEN THE Admin_Auth_System SHALL 유효성 검증 실패 에러(COMMON4000, 400)를 반환한다.
9. WHEN 로그인이 성공하면, THE Admin_Auth_System SHALL 응답 코드 "AUTH2001"과 메시지 "로그인에 성공했습니다."를 반환한다.

## API Specification

### POST /api/admin/auth/login

**Request Header:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "loginId": "dev_admin",
  "password": "sofit1234!"
}
```

**Response 200 (성공):**
```json
{
  "isSuccess": true,
  "code": "AUTH2001",
  "message": "로그인에 성공했습니다.",
  "result": {
    "userId": 1,
    "name": "김관리",
    "role": "ADMIN_DEV"
  }
}
```

**Response 400 (로그인 실패):**
```json
{
  "isSuccess": false,
  "code": "AUTH4001",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```
