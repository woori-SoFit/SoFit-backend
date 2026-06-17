# CSRF 토큰 발급 활성화

## 개요
현재 `csrf.disable()`로 비활성화된 CSRF 보호를 `HttpSessionCsrfTokenRepository` 기반으로 변경합니다.
CSRF 토큰은 Redis 세션에 저장되며, 프론트엔드는 전용 엔드포인트에서 토큰을 받아 `X-CSRF-TOKEN` 헤더로 전송합니다.

## 브랜치
- 브랜치명: `feat/SOFIT-XXX-csrf-token-enable`
- 커밋: `[SOFIT-XXX] Feat: CSRF 토큰 발급 활성화`

---

## Phase 1: CSRF 토큰 설정 변경

### 작업 내용
1. **sofit-user SecurityConfig** — CSRF 활성화 + HttpSessionCsrfTokenRepository 설정
2. **sofit-admin SecurityConfig** — CSRF 활성화 + HttpSessionCsrfTokenRepository 설정
3. **sofit-user CsrfTokenController** — GET `/api/csrf-token` 엔드포인트 (토큰 발급)
4. **sofit-admin CsrfTokenController** — GET `/api/admin/csrf-token` 엔드포인트 (토큰 발급)

### 설계 방향
- `HttpSessionCsrfTokenRepository` 사용 → CSRF 토큰이 Redis 세션에 저장됨
- 프론트엔드는 GET 엔드포인트로 토큰을 응답 본문에서 받음 (쿠키 사용 안 함)
- 프론트엔드는 상태 변경 요청(POST/PUT/DELETE/PATCH) 시 `X-CSRF-TOKEN` 헤더에 토큰 포함
- GET, HEAD, OPTIONS, TRACE 요청은 CSRF 검증 제외 (Spring Security 기본 동작)
- CSRF 토큰 발급 엔드포인트는 permitAll 처리

### 검증 흐름
```
1. 클라이언트 → GET /api/csrf-token → 서버: 세션 생성 + 토큰 저장 + 응답 본문으로 토큰 전달
2. 클라이언트 → POST /api/xxx (X-CSRF-TOKEN 헤더에 토큰 포함)
3. 서버: 세션에서 저장된 토큰 vs 헤더 토큰 비교 → 일치하면 통과
```

### 멀티 인스턴스 대응
- Spring Session + Redis로 세션이 공유되므로 어떤 인스턴스로 요청이 가도 동일한 토큰 검증 가능

### 영향 범위
- 프론트엔드: 페이지 로드 시 GET으로 토큰 발급 → 이후 모든 상태 변경 요청에 `X-CSRF-TOKEN` 헤더 추가 필요
- API Dog 테스트: CSRF 토큰을 먼저 GET으로 받고, 이후 요청 헤더에 포함해야 함
