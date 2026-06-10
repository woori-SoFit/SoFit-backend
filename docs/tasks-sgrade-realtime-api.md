# 회원가입 시 S등급 실시간 산출 (FastAPI 연동)

- 브랜치: `feat/SOFIT-179-sgrade-realtime-predict`
- 커밋: `[SOFIT-179] Feat: 회원가입 시 Python AI 서버 호출하여 S등급 실시간 산출`

---

## 개요

- 회원가입 완료 후 비동기로 Python AI 서버에 S등급 산출 요청
- 회원가입 응답은 즉시 반환 (사용자는 화면 전환하여 다른 일 수행)
- AI 서버 호출은 백그라운드에서 최대 3회 재시도
- 3회 실패 시 s_grade_history.status = FAILED로 기록 (수동 배치 대상)

---

## Phase 1: AI Client 구현

### 1-1. SGradeAiClient 인터페이스 + 구현체
- 파일: `sofit-user/.../sgrade/client/SGradeAiClient.java`
- 파일: `sofit-user/.../sgrade/client/SGradeAiClientImpl.java`
- RestTemplate으로 `POST {ai-server-url}/api/s-grade/predict` 호출
- Request: `{ "biz_data_id": Long }`
- Response DTO: SGradePredictResponse (s_grade, target_grade, keywords, details, advice)

### 1-2. application.yml에 AI 서버 URL 설정
- `sofit.ai.server.url` 프로퍼티 추가

---

## Phase 2: 비동기 S등급 산출 서비스

### 2-1. SGradeService 구현
- 파일: `sofit-user/.../sgrade/service/SGradeService.java`
- 파일: `sofit-user/.../sgrade/service/SGradeServiceImpl.java`
- `@Async` 메서드로 비동기 실행
- 로직:
  1. userId → BusinessProfile.businessNumber 조회
  2. businessNumber → 최신 MyBizData.bizDataId 조회
  3. AI 서버 호출 (최대 3회 재시도, 간격 1초)
  4. 성공 시: s_grade_history COMPLETED 업데이트 + s_grade_report INSERT
  5. 실패 시: s_grade_history FAILED 업데이트

### 2-2. AsyncConfig 추가
- 파일: `sofit-user/.../global/config/AsyncConfig.java`
- `@EnableAsync` + TaskExecutor 설정

---

## Phase 3: 회원가입 플로우 연동

### 3-1. AuthServiceImpl 수정
- 기존: `SGradeHistory.createRequested(user)` 저장 후 끝
- 변경: 저장 후 `sGradeService.predictAsync(user.getUserId(), sGradeHistory.getSGradeId())` 비동기 호출

---

## Phase 4: SGradeHistory 엔티티 메서드 추가

### 4-1. SGradeHistory에 상태 변경 메서드 추가
- `markCompleted(Long featureId)`: status=COMPLETED, featureId 연결, evaluatedAt 기록
- `markFailed()`: status=FAILED

---

## 확인 사항
- [ ] 회원가입 응답 시간에 영향 없음 (비동기)
- [ ] AI 서버 장애 시 회원가입 정상 완료
- [ ] 3회 실패 시 FAILED 기록됨
- [ ] 성공 시 s_grade_report에 정상 저장
