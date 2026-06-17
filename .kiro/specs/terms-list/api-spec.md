# API 명세 — 약관 목록 조회

## 공통 정보

- **모듈**: `sofit-user`
- **도메인**: `terms` (loan 하위 아닌 별도 도메인)
- **인증**: Bearer accessToken (현재 임시 permitAll이면 유지, TODO 주석)
- **응답 포맷**: 프로젝트 공통 `ApiResponse<T>`

---

## API. 약관 목록 조회

### Request

```
GET /api/terms?termType=LOAN_APPLICATION
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| termType | Query | Enum (TermType) | Y | 조회할 약관 타입 |

### Response (200 OK)

```json
{
  "isSuccess": true,
  "code": "TERM2000",
  "message": "약관 목록 조회에 성공했습니다.",
  "result": {
    "terms": [
      {
        "termId": 1,
        "termType": "LOAN_APPLICATION",
        "version": "v1.0",
        "title": "개인(신용)정보 조회 동의",
        "fileUrl": "http://localhost:8080/terms/loan_application_v1.0.pdf",
        "isRequired": true,
        "isActive": true,
        "effectiveAt": "2026-05-12T00:00:00"
      }
    ]
  }
}
```

### Response 필드

| 필드명 | 타입 | 설명 |
|---|---|---|
| termId | Long | 약관 ID |
| termType | String (Enum) | 약관 타입 |
| version | String | 약관 버전 |
| title | String | 약관 명 |
| fileUrl | String | PDF 파일 전체 URL (base-url + 상대 경로 조립) |
| isRequired | Boolean | 필수 여부 |
| isActive | Boolean | 현행 여부 |
| effectiveAt | LocalDateTime | 시행 일시 |

### 조회 조건

- `term_type = {termType}` AND `is_active = true` (현행 버전만 조회)

### PDF 파일 URL 조립

- DB에는 상대 경로(`/terms/xxx.pdf`)만 저장
- 응답 시 `sofit.storage.base-url` + 상대 경로로 조립하여 `fileUrl` 반환
- MVP: Spring Boot 정적 리소스 (`sofit-user/src/main/resources/static/terms/`)
- 향후 AWS 이전: `base-url`을 S3 도메인으로 교체

### Error Response

| 상황 | HTTP | code | message |
|---|---|---|---|
| 유효하지 않은 termType | 400 | COMMON4000 | 잘못된 요청입니다. |

- 유효하지 않은 termType은 Spring의 `@RequestParam` Enum 바인딩 실패 시 `MethodArgumentTypeMismatchException`으로 처리됨
- 별도 도메인 ErrorCode 불필요 (공통 COMMON4000 사용)
