-- ============================================================
-- SoFit 감사 로그(접근기록) 트랙 — 테이블 + append-only 트리거
-- 전자금융감독규정 접근기록 요건 (누가/언제/무엇을/어떻게/결과)
--
-- 적용 방법 (대상 DB에 접속한 상태에서 실행):
--   운영(dev):  mysql -h <DB_HOST> -u root -p sofit          < audit_log.sql
--   테스트:     mysql -h <DB_HOST> -u root -p sofit_test_v3   < audit_log.sql
-- ============================================================

-- 1) 감사 로그 테이블 (AuditLogEntity 매핑 → ddl-auto:validate 대상, 앱 배포 "전에" 적용 필수)
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    event_time    DATETIME(6)  NOT NULL                COMMENT '언제 (NTP 동기화)',
    actor         VARCHAR(100) NOT NULL                COMMENT '누가 (로그인ID / BATCH / SYSTEM)',
    actor_role    VARCHAR(50)  NULL                    COMMENT '권한',
    action        VARCHAR(100) NOT NULL                COMMENT '무엇을 (LOAN_APPROVE 등)',
    target        VARCHAR(200) NULL                    COMMENT '대상',
    source_system VARCHAR(20)  NOT NULL                COMMENT '어느 서버 (USER/ADMIN/BATCH)',
    access_method VARCHAR(20)  NULL                    COMMENT '어떻게 (WEB/BATCH)',
    client_ip     VARCHAR(45)  NULL                    COMMENT '접근 IP (IPv6 포함)',
    result        VARCHAR(20)  NOT NULL                COMMENT 'SUCCESS / FAILURE (실패도 기록)',
    trace_id      VARCHAR(64)  NULL                    COMMENT '추적 ID (traceId)',
    PRIMARY KEY (id),
    KEY idx_audit_event_time (event_time),
    KEY idx_audit_actor (actor),
    KEY idx_audit_action (action),
    KEY idx_audit_target (target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='접근기록(감사 로그) — append-only';

-- 2) append-only 트리거 — UPDATE/DELETE 시도 시 SQLSTATE 45000으로 거부
--    앱 계정(sofit)을 포함한 모든 계정의 변조·삭제 시도를 DB 레벨에서 차단한다.
--    ※ root 계정은 트리거를 DROP할 수 있으므로 root 직접 접근은 별도 통제 필요
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_audit_no_update
BEFORE UPDATE ON audit_log
FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only (UPDATE denied)';
//
CREATE TRIGGER IF NOT EXISTS trg_audit_no_delete
BEFORE DELETE ON audit_log
FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only (DELETE denied)';
//
DELIMITER ;

-- 검증:
--   INSERT 테스트 후 UPDATE/DELETE 시도 → 45000 에러로 거부되면 정상
--   SHOW TRIGGERS WHERE `Table` = 'audit_log';
