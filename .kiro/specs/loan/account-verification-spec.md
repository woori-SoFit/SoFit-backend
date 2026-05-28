# 계좌 인증 (1원 이체) 개발 명세서

## 개요

사용자가 대출 실행 계좌를 등록할 때, 본인 명의 계좌인지 확인하기 위해 1원 이체 방식의 계좌 인증을 수행한다.
코데프(CODEF)의 계좌 인증(1원 이체) API를 활용하며, 인증 흐름은 아래 두 단계로 구성된다.

1. **1원 송금 요청** - 사용자가 입력한 계좌로 1원을 송금하고, 입금자명에 인증코드를 포함시킨다.
2. **1원 인증 확인** - 사용자가 통장에서 확인한 인증코드를 입력하면 서버에서 일치 여부를 검증한다.

---

## 전체 플로우

```
[사용자] 은행 선택 + 계좌번호 입력
        ↓
[프론트] POST /api/account-verification
        ↓
[서버] 세션에서 applicationId 추출
        ↓
[서버] 코데프 API 호출
       - organization: bankCode
       - account: accountNumber
       - inPrintType: "0" (4자리 랜덤숫자)
        ↓
[코데프] 해당 계좌로 1원 송금
        입금자명에 authCode(4자리) 포함
        ↓
[서버] authCode를 Redis에 저장
       - Key: applicationId
       - Value: authCode
       - TTL: 5분
        ↓
[프론트] 마스킹된 계좌번호, 예금주명, 만료시각 화면에 표시
        ↓
[사용자] 통장 입금자명에서 4자리 확인 후 입력
        ↓
[프론트] POST /api/account-verification/confirm
        ↓
[서버] 세션에서 applicationId 추출
       → Redis에서 authCode 조회
       → verificationCode와 비교
        ↓
[서버] 일치 시 Redis 삭제
       loan_execution 테이블에 account_number, bank_code 저장
        ↓
[프론트] 계좌 인증 완료 처리
```

---

## 외부 API: 코데프 계좌 인증(1원 이체)

### Endpoint

| 환경 | URL |
|------|-----|
| 데모 | `https://development.codef.io/v1/kr/bank/a/account/transfer-authentication` |
| 운영 | `https://api.codef.io/v1/kr/bank/a/account/transfer-authentication` |

- HTTP 메서드: `POST`
- Timeout: `30초`

### Request Body

```json
{
  "organization": "020",
  "account": "1002940540000",
  "inPrintType": "0",
  "inPrintContent": ""
}
```

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| organization | String | Y | 은행 기관코드 (코데프 기관코드 기준) |
| account | String | Y | 계좌번호 (-없이 숫자만) |
| inPrintType | String | Y | 입금자명 타입, 기본값 `0` |
| inPrintContent | String | △ | inPrintType이 `2` 또는 `9`일 때만 설정 |

**inPrintType 상세**

| 값 | 설명 | 예시 |
|----|------|------|
| `0` | 4자리 랜덤숫자 | `5673` |
| `1` | 랜덤문자 | `빨간기린` |
| `2` | 고객사키워드 + 3자리 랜덤숫자 | `CODEF842` |
| `9` | 고객사 직접 입력 | `사용자지정` |

> 본 서비스는 `inPrintType: "0"` 고정 사용 (화면 UX가 4자리 숫자 입력 기준)

### Response Body

```json
{
  "authCode": "5673"
}
```

| 필드명 | 타입 | 설명 |
|--------|------|------|
| authCode | String | 사용자 통장 입금자명에 표시되는 인증코드 |

### 주의사항

- 은행 서비스 점검 시간에는 호출 불가
- 계좌번호당 **1일 5회** 요청 제한
- 데모 서버는 실제 인증코드가 조회되지 않으며, 랜덤 테스트 데이터 반환

---

## API 1: 1원 송금 요청

| 항목 | 내용 |
|------|------|
| 분류 | 대출 |
| API Path | `/api/account-verification` |
| HTTP 메서드 | `POST` |
| 백 담당 | 희연 고 |
| 백엔드 개발 현황 | 시작 전 |
| 프론트엔드 개발 현황 | 시작 전 |

### Request Header

| Key | Value |
|-----|-------|
| Cookie | `JSESSIONID={sessionId}` |

### Request Body

```json
{
  "bankCode": "020",
  "accountNumber": "1002940540000"
}
```

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| bankCode | String | Y | 은행 기관코드 (코데프 기관코드 기준) |
| accountNumber | String | Y | 계좌번호 (-없이 숫자만) |

### 서버 내부 처리

1. 세션에서 `applicationId` 추출
2. 코데프 API 호출
   - `bankCode` → `organization` 매핑
   - `accountNumber` → `account` 매핑
   - `inPrintType`: `"0"` 고정
3. 코데프 응답의 `authCode`를 Redis에 저장
   - Key: `applicationId`
   - Value: `authCode`
   - TTL: 5분

### Response Body

**2000 성공**

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {
    "bankName": "우리은행",
    "maskedAccountNumber": "1002-****-540000",
    "accountHolder": "홍길동",
    "expiredAt": "2026-05-09T10:35:00"
  }
}
```

| 필드명 | 타입 | 설명 |
|--------|------|------|
| bankName | String | 은행명 |
| maskedAccountNumber | String | 마스킹된 계좌번호 (화면 표시용) |
| accountHolder | String | 예금주명 (본인 명의 확인용) |
| expiredAt | String | 인증 만료 시각 (5분, ISO 8601) |

**실패**

```json
// 유효하지 않은 계좌번호
{
  "isSuccess": false,
  "code": "ACCOUNT4001",
  "message": "유효하지 않은 계좌번호입니다."
}

// 일일 요청 한도 초과
{
  "isSuccess": false,
  "code": "ACCOUNT4002",
  "message": "오늘 인증 요청 가능 횟수를 초과했습니다."
}

// 코데프 API 오류
{
  "isSuccess": false,
  "code": "ACCOUNT5001",
  "message": "계좌 인증 서비스에 일시적인 오류가 발생했습니다."
}
```

---

## API 2: 1원 인증 확인

| 항목 | 내용 |
|------|------|
| 분류 | 대출 |
| API Path | `/api/account-verification/confirm` |
| HTTP 메서드 | `POST` |
| 백 담당 | 희연 고 |
| 백엔드 개발 현황 | 시작 전 |
| 프론트엔드 개발 현황 | 시작 전 |

### Request Header

| Key | Value |
|-----|-------|
| Cookie | `JSESSIONID={sessionId}` |

### Request Body

```json
{
  "verificationCode": "5673"
}
```

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| verificationCode | String | Y | 사용자가 입력한 4자리 인증번호 |

### 서버 내부 처리

1. 세션에서 `applicationId` 추출
2. Redis에서 `applicationId`로 `authCode` 조회
3. `verificationCode`와 `authCode` 비교
4. 일치 시 → Redis 삭제, `loan_execution` 테이블에 `account_number`, `bank_code` 저장

### Response Body

**2000 성공**

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {
    "accountVerified": true
  }
}
```

| 필드명 | 타입 | 설명 |
|--------|------|------|
| accountVerified | Boolean | 계좌 인증 완료 여부 |

> 인증 완료 시 `loan_execution` 테이블에 `account_number`(`loan_execution.account_number`), `bank_code`(`loan_execution.bank_code`) 저장

**실패**

```json
// 인증코드 불일치
{
  "isSuccess": false,
  "code": "ACCOUNT4003",
  "message": "인증번호가 일치하지 않습니다."
}

// 인증 만료 (Redis TTL 5분 초과)
{
  "isSuccess": false,
  "code": "ACCOUNT4004",
  "message": "인증 시간이 만료되었습니다. 다시 요청해주세요."
}
```

---

## Redis 구조 요약

| 항목 | 내용 |
|------|------|
| Key | `applicationId` |
| Value | `authCode` (4자리 숫자 문자열) |
| TTL | 300초 (5분) |
| 저장 시점 | 코데프 API 응답 수신 직후 |
| 삭제 시점 | 인증 성공 시 즉시 삭제 (TTL 만료 전이라도) |

---

## 에러 코드 정리

| 코드 | 상황 |
|------|------|
| `ACCOUNT4001` | 유효하지 않은 계좌번호 |
| `ACCOUNT4002` | 일일 요청 한도 초과 (5회) |
| `ACCOUNT4003` | 인증번호 불일치 |
| `ACCOUNT4004` | 인증 시간 만료 |
| `ACCOUNT5001` | 코데프 API 오류 |
