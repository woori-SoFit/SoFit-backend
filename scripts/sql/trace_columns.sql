-- ============================================================
-- SoFit DB 경계 추적 컬럼 (분산 추적 흔적)
-- user / admin / batch 는 호출 관계 없이 DB만 공유 → 행(row)에 요청 흔적을 남긴다.
-- TraceableEntity(@PrePersist)가 MDC(traceId)에서 자동으로 채움.
--
-- ※ source_system 은 loan_decision.status(SYSTEM_*/TELLER_*/MANAGER_*)가 이미 작성 주체를
--   담고 있어 중복 → trace_id 단일 컬럼만 추가.
--
-- ⚠️ ddl-auto: validate 이므로, 이 스크립트를 앱 배포 "전에" 적용해야 부팅된다.
-- 적용 (대상 DB 접속 상태):
--   운영(dev):  mysql -h <DB_HOST> -u root -p sofit          < trace_columns.sql
--   테스트:     mysql -h <DB_HOST> -u root -p sofit_test_v2   < trace_columns.sql
--   ※ InnoDB Cluster(Primary=db1)에서 실행 → Secondary 자동 복제
-- ============================================================

-- loan_decision : admin 승인/반려 + 배치(SYSTEM_*) 가 INSERT
ALTER TABLE loan_decision
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT '작성 요청의 traceId (로그와 동일 값)' AFTER created_by;

-- loan_application : user 가 INSERT
ALTER TABLE loan_application
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT '작성 요청의 traceId (로그와 동일 값)';

-- loan_execution : user 계좌 인증 확정 시 INSERT
ALTER TABLE loan_execution
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT '작성 요청의 traceId (로그와 동일 값)';

-- (선택) 조회 편의 인덱스 — trace_id 로 교차 추적 시
-- CREATE INDEX idx_loan_decision_trace_id    ON loan_decision(trace_id);
-- CREATE INDEX idx_loan_application_trace_id ON loan_application(trace_id);

-- 검증:
--   DESC loan_decision;     -- trace_id 컬럼 확인
--   DESC loan_application;
--   -- 신청/승인 1건씩 발생시킨 뒤:
--   SELECT decision_id, trace_id, created_at FROM loan_decision ORDER BY decision_id DESC LIMIT 5;
