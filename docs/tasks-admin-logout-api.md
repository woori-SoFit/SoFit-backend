# 관리자 페이지 로그아웃 API

## 개요

| 항목 | 내용 |
|---|---|
| API Path | `POST /api/admin/auth/logout` |
| 브랜치명 | `feat/SOFIT-162-admin-logout` |
| 커밋 메시지 | `[SOFIT-162] Feat: 관리자 페이지 로그아웃 API 구현` |
| PR 베이스 | `dev` |

## Phase 1: 관리자 로그아웃 API 구현

| # | 작업 | 파일 |
|---|---|---|
| 1 | `SessionUtil` 유틸 클래스 생성 (세션 무효화 + SecurityContext 클리어 + 쿠키 만료) | `sofit-admin/src/main/java/com/sofit/admin/global/util/SessionUtil.java` |
| 2 | `AdminAuthSuccessCode`에 `LOGOUT_SUCCESS` 코드 추가 (`AUTH2004`, "로그아웃에 성공했습니다.") | `sofit-admin/src/main/java/com/sofit/admin/domain/auth/exception/AdminAuthSuccessCode.java` |
| 3 | `AdminAuthService` 인터페이스에 `logout` 메서드 시그니처 추가 | `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthService.java` |
| 4 | `AdminAuthServiceImpl`에 `logout` 구현 (SessionUtil 호출) | `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthServiceImpl.java` |
| 5 | `AdminAuthControllerDocs`에 logout Swagger 문서 추가 | `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthControllerDocs.java` |
| 6 | `AdminAuthController`에 `POST /logout` 엔드포인트 추가 | `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthController.java` |

## 구현 상세

### 인증 요구사항
- 로그인된 관리자(ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV)만 호출 가능
- 인증되지 않은 사용자는 Spring Security에서 자동으로 401 반환 (`COMMON4001`)

### 로그아웃 처리 로직
1. 현재 세션 무효화 (`session.invalidate()`)
2. SecurityContext 클리어 (`SecurityContextHolder.clearContext()`)
3. 세션 쿠키 만료 처리 (SESSION, JSESSIONID)

### 응답 명세

**200 성공**
```json
{
  "isSuccess": true,
  "code": "AUTH2004",
  "message": "로그아웃에 성공했습니다."
}
```

**401 인증되지 않은 사용자**
```json
{
  "isSuccess": false,
  "code": "COMMON4001",
  "message": "인증이 필요합니다."
}
```

## Git 명령어 안내

```bash
git checkout -b feat/SOFIT-162-admin-logout
git add .
git commit -m "[SOFIT-162] Feat: 관리자 페이지 로그아웃 API 구현"
git push -u origin feat/SOFIT-162-admin-logout
```
