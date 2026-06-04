# DRAFT 삭제 API 정리

---

## 📌 개요

대출 신청 중 사용자가 신청을 취소할 경우 DRAFT 상태의 신청서를 삭제 처리한다.
실제 DB row를 삭제하지 않고 `status = CANCELLED`로 변경한다.

---

## 💡 실제 삭제(row 삭제) 대신 상태 변경을 하는 이유

- 신청 이력이 사라지면 감사(audit) 추적 불가 — 금융 서비스에서 중요
- 나중에 "왜 이 신청이 없어졌지?" 같은 문의 대응 불가
- DRAFT라도 생성된 순간부터 이력으로 남기는 것이 금융권 관행

---

## 📋 ApplicationStatus ENUM

```java
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    CB_CHECKING,
    BASIC_REVIEW,
    S_CALCULATING,
    S_COMPLETED,
    SYSTEM_APPROVED,
    SYSTEM_REJECTED,
    MANAGER_REVIEW,
    APPROVED,
    REJECTED,
    CONTRACTED,
    EXECUTED,
    CANCELLED   // DRAFT 취소 및 대출 취소 공통 사용
                // (현재 서비스에서 대출 실행 후 취소 기능 없음)
}
```

---

## 🔌 API 상세

```
DELETE /api/loan-applications/{applicationId}
```

**처리 흐름**
```
1. applicationId 존재 여부 확인
2. 본인 소유 확인 (세션 userId 비교)
3. status = DRAFT 확인 (DRAFT가 아니면 에러)
4. status = CANCELLED 변경
```

**Response**
```json
{
  "isSuccess": true,
  "code": "LOAN2XXX",
  "message": "신청서가 취소되었습니다."
}
```

---

## ⚠️ 다른 로직에서의 영향

`CANCELLED`는 `DRAFT`가 아니므로 기존 DRAFT 조회 로직에서 **자동으로 걸러짐**

```java
// 기존 DRAFT 존재 여부 확인 쿼리 — 별도 수정 불필요
loanApplicationRepository.findByUserIdAndProductIdAndStatus(userId, productId, DRAFT);
```
