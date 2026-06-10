# Python AI 서버 API 스펙 (S등급 산출)

## 변경 배경

- **기존**: Python 배치(crontab)가 DB에 직접 접근하여 S등급 산출 + 저장
- **변경**: Spring BE가 회원가입 시 Python FastAPI를 HTTP 호출 → 응답 받아서 Spring이 DB 저장
- **Python의 역할**: DB에서 feature 읽기 → 예측 + SHAP + LLM → 결과를 응답으로 반환 (DB 쓰기 안 함)

---

## API 엔드포인트

```
POST /api/s-grade/predict
Content-Type: application/json
```

---

## Request (Spring BE → Python)

```json
{
  "biz_data_id": 1
}
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `biz_data_id` | int | ✅ | `s_grade_feature` 테이블에서 피처를 조회할 key |

> Spring BE는 식별자만 보냅니다. Python이 DB에서 `s_grade_feature`를 직접 SELECT하여 모델 입력으로 사용합니다.

---

## Python 내부 처리 흐름

```
1. biz_data_id로 s_grade_feature 테이블 SELECT (33개 피처)
2. LGBM 모델에 입력 → S등급(S1~S10) 예측
3. SHAP 값 계산 → 강점/개선 항목 분류 및 키워드 추출
4. SHAP 결과를 LLM(Gemini)에 전달 → user_advice, admin_advice 자연어 생성
5. 결과를 JSON Response로 반환
```

---

## Response (Python → Spring BE)

### 성공 (200 OK)

```json
{
  "s_grade": "S3",
  "target_grade": "S2",
  "strength_keywords": ["매출 성장", "고객 재방문율"],
  "improvement_keywords": ["현금흐름", "비용 구조"],
  "strength_details": {
    "매출 성장": 0.35,
    "고객 재방문율": 0.22
  },
  "improvement_details": {
    "현금흐름": -0.18,
    "비용 구조": -0.12
  },
  "user_advice": "매출 성장세를 유지하면서 현금흐름 관리를 강화하세요. 비용 절감과 함께 안정적인 수익 구조를 만들어보세요.",
  "admin_advice": "매출 성장률 상위 20%. quarterly_revenue_growth_rate=11.54로 업종 평균 대비 1.75배. 현금흐름 지표 개선 필요 (max_inactive_days=3, 거래 연속성 양호하나 cash_flow 대비 outflow 비율 주의)."
}
```

### 응답 필드 설명

| 필드 | 타입 | nullable | 설명 |
|------|------|----------|------|
| `s_grade` | string | ❌ | 산출된 등급 (S1~S10) |
| `target_grade` | string | ✅ | 다음 목표 등급 (S1이면 null) |
| `strength_keywords` | list[string] | ❌ | 강점 키워드 목록 (고객 리포트용) |
| `improvement_keywords` | list[string] | ❌ | 개선 키워드 목록 (고객 리포트용) |
| `strength_details` | dict[string, float] | ❌ | 강점 항목별 SHAP 기여도 |
| `improvement_details` | dict[string, float] | ❌ | 개선 항목별 SHAP 기여도 (음수) |
| `user_advice` | string | ❌ | 고객용 자연어 조언 (LLM 생성) |
| `admin_advice` | string | ❌ | 은행원용 상세 분석 (내부 변수 포함 가능) |

### 실패 (4xx / 5xx)

```json
{
  "error": "FEATURE_NOT_FOUND",
  "message": "biz_data_id=99에 해당하는 feature 데이터가 없습니다."
}
```

```json
{
  "error": "MODEL_PREDICTION_FAILED",
  "message": "모델 예측 중 오류가 발생했습니다."
}
```

---

## 호출 시점

- **회원가입 완료 시** (My Biz Data가 이미 존재하는 상태)
- Spring BE가 해당 사용자의 최신 `my_biz_data.biz_data_id`를 조회하여 전달

---

## Python 서버의 DB 접근 범위

| 동작 | 대상 테이블 | 권한 |
|------|-------------|------|
| ✅ SELECT | `s_grade_feature` | 읽기 |
| ❌ INSERT/UPDATE | `s_grade_history` | 없음 |
| ❌ INSERT/UPDATE | `s_grade_report` | 없음 |

---

## 데이터 전제 조건

- `my_biz_data`와 `s_grade_feature`는 **처음부터 존재**한다고 가정
- 파생 로직은 없음 (둘 다 Mock/더미 데이터로 미리 적재)
- `s_grade_feature.biz_data_id`로 어떤 my_biz_data 기반인지 추적

---

## 참고: Spring BE가 응답 받은 후 저장하는 테이블

| 테이블 | 저장 내용 |
|--------|-----------|
| `s_grade_history` | status를 COMPLETED로 업데이트, feature_id 연결, evaluated_at 기록 |
| `s_grade_report` | 등급, target_grade, 키워드, 상세, 조언 저장 |
