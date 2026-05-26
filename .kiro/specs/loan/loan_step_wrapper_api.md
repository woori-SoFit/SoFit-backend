# 대출 신청 플로우 - 단계별 래퍼 API 정리

---

## 🪜 단계별 API 전체 흐름

| 단계 | last_completed_step | resumeStep | API |
|---|---|---|---|
| 신청 생성 직후 | `null` | `CONSENT` | `POST /api/loan-products/{productId}/applications` |
| 약관 동의 완료 | `CONSENT_DONE` | `AUTH` | `POST /api/loan-applications/{applicationId}/consents` |
| 본인인증 완료 | `AUTH_DONE` | `BIZ_INFO` | `POST /api/loan-applications/{applicationId}/verify-pin` |
| 사업자 정보 확인 완료 | `BIZ_INFO_DONE` | `COLLECT_DATA` | `POST /api/loan-applications/{applicationId}/biz-info` |
| 마이데이터 수집 완료 | `DATA_COLLECTED` | `MYBIZ` | `POST /api/loan-applications/{applicationId}/my-data` |
| 마이비즈데이터 연동 완료 | `MYBIZ_CONNECTED` | `LOAN_CONDITION` | `POST /api/loan-applications/{applicationId}/mybiz-data` |
| 최종 제출 | `MYBIZ_CONNECTED` (유지) | - | `POST /api/loan-applications/{applicationId}/submit` |

---

## 🔌 단계별 래퍼 API 상세

### Step 1. 대출 신청 생성 (DRAFT 생성)

```
POST /api/loan-products/{productId}/applications
```

```
1. 고객 입력값 저장 (annualIncome, creditScore, incomeType, existingLoanAmt)
2. status = DRAFT
3. lastCompletedStep = null
```

**Response**
```json
{ "applicationId": 1 }
```

---

### Step 2. 대출 약관 동의

```
POST /api/loan-applications/{applicationId}/consents
```

```
1. consentService.agree(대출 약관 목록)  // 범용 서비스 직접 호출
2. lastCompletedStep = CONSENT_DONE
```

> 범용 약관 동의 서비스를 재활용하되, lastCompletedStep 업데이트는 래퍼에서 담당

---

### Step 3. 본인인증 (금융인증서)

```
POST /api/loan-applications/{applicationId}/verify-pin
```

```
1. authService.verify()  // 범용 본인인증 서비스 직접 호출
2. lastCompletedStep = AUTH_DONE
```

> 본인인증 서비스는 다른 곳에서도 재활용되므로 래퍼로 분리

---

### Step 4. 사업자 정보 확인

```
POST /api/loan-applications/{applicationId}/biz-info
```

```
1. bizInfoService.getBizInfo(bizNo)  // 범용 서비스 직접 호출
2. lastCompletedStep = BIZ_INFO_DONE
```

> 범용 API는 GET이지만 래퍼는 POST — 이유:
> - "사업자 정보를 조회한다"가 아니라 "사업자 정보 확인 단계를 완료한다"는 행위
> - lastCompletedStep 업데이트로 서버 상태가 변경되므로 POST가 의미적으로 맞음
> - 내부적으로 HTTP 재호출(RestTemplate 등) 하지 않고 서비스 레이어 직접 호출

---

### Step 5. 마이데이터 수집

```
POST /api/loan-applications/{applicationId}/my-data
```

```
1. consentService.agree(마이데이터 약관 목록)  // 범용 서비스 직접 호출 (Step 2와 동일 서비스)
2. 마이데이터 수집 처리
3. lastCompletedStep = DATA_COLLECTED
```

> Step 2와 동일한 범용 약관 서비스를 호출하지만 의미와 책임이 다르므로 래퍼 분리

---

### Step 6. 마이비즈데이터 연동

```
POST /api/loan-applications/{applicationId}/mybiz-data
```

```
1. mybizService.collect(bizNo)  // 범용 서비스 직접 호출
2. lastCompletedStep = MYBIZ_CONNECTED
```

> 범용 마이비즈 조회 API는 GET이지만 래퍼는 POST — Step 4와 동일한 이유

---

### Step 7. 최종 제출 (심사 요청)

```
POST /api/loan-applications/{applicationId}/submit
```

```
1. validation
   - status = DRAFT 확인
   - lastCompletedStep = MYBIZ_CONNECTED 확인 (단계 건너뜀 방지)
   - term 유효성 검증 (purpose + repaymentMethod 조합 기준)
   - requestedAmount 유효성 검증 (상품 max_limit 이내)
2. requested_amount, term, repayment_method, purpose 저장
3. bankerAssignmentService.assignBanker() 호출 → assignedBankerId 배정
4. status = SUBMITTED
5. applied_at = 현재 시각
6. lastCompletedStep 변경 없음 (MYBIZ_CONNECTED 유지)
```

**Response**
```json
{
  "applicationId": 1,
  "productName": "카카오뱅크 개인사업자 신용대출",
  "requestedAmount": 100000000,
  "appliedAt": "2024-05-12T14:35:00",
  "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST"
}
```

---

## 💡 래퍼 API 설계 원칙

**범용 서비스는 lastCompletedStep을 모른다**
- 약관 동의, 본인인증 등 범용 서비스는 대출 신청 컨텍스트를 알 필요 없음
- `lastCompletedStep` 업데이트는 항상 대출 신청 래퍼 API/서비스에서 담당

**각 래퍼 API는 하나의 단계만 책임진다**
- 내부적으로 같은 서비스를 호출하더라도 단계의 의미가 다르면 API를 분리
- ex) `/consents`와 `/my-data`는 동일한 범용 약관 서비스를 쓰지만 별도 API로 분리

**lastCompletedStep은 각 래퍼 API 완료 시점에 업데이트**
- 별도로 `lastCompletedStep`만 업데이트하는 API는 만들지 않음
- 각 단계 처리와 `lastCompletedStep` 업데이트를 한 트랜잭션으로 처리

**서버 상태가 변경되면 POST 사용**
- `lastCompletedStep` 업데이트가 발생하는 단계는 범용 API가 GET이어도 래퍼는 POST 사용
- ex) 사업자 정보 확인, 마이비즈데이터 연동

**내부 서비스 호출은 HTTP가 아닌 서비스 레이어 직접 호출**
- 래퍼 API 내부에서 다른 API를 HTTP로 재호출하지 않음 (불필요한 네트워크 비용)
- 범용 API와 동일한 서비스 레이어를 직접 호출
