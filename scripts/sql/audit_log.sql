-- ============================================================
-- SoFit 감사 로그(접근기록) 트랙 — 테이블 + append-only 전용 계정
-- 전자금융감독규정 접근기록 요건 (누가/언제/무엇을/어떻게/결과)
--
-- 적용 방법 (대상 DB에 접속한 상태에서 실행):
--   운영(dev):  mysql -h <DB_HOST> -u root -p sofit          < audit_log.sql
--   테스트:     mysql -h <DB_HOST> -u root -p sofit_test_v2   < audit_log.sql
--   ※ <AUDIT_WRITER_PASSWORD> 를 실제 비밀번호로 치환 후 실행할 것 (평문 커밋 금지)
-- ============================================================

-- 1) 감사 로그 테이블 (JPA 엔티티 아님 → ddl-auto:validate 대상에서 제외됨)
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
    -- 무결성 컬럼 (다음 이터레이션에서 채움 — 지금은 NULL 허용으로 선반영)
    hmac          VARCHAR(64)  NULL                    COMMENT 'HMAC 서명 (변조 탐지)',
    prev_hash     VARCHAR(64)  NULL                    COMMENT '직전 레코드 해시 (해시 체인)',
    PRIMARY KEY (id),
    KEY idx_audit_event_time (event_time),
    KEY idx_audit_actor (actor),
    KEY idx_audit_action (action),
    KEY idx_audit_target (target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='접근기록(감사 로그) — append-only';

-- 2) 감사 전용 계정 (직무 분리: 앱 계정 sofit 과 분리)
--    INSERT/SELECT 만 부여 → UPDATE/DELETE 권한이 없어 변조·삭제 불가 (append-only 강제)
CREATE USER IF NOT EXISTS 'audit_writer'@'%' IDENTIFIED BY '<AUDIT_WRITER_PASSWORD>';

-- 현재 접속 DB(sofit / sofit_test_v2)의 audit_log 에만 권한 부여
GRANT INSERT, SELECT ON audit_log TO 'audit_writer'@'%';

-- (선택) 2차 방어선: UPDATE/DELETE 를 트리거로도 거부
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

FLUSH PRIVILEGES;

-- 검증:
--   SHOW GRANTS FOR 'audit_writer'@'%';   -- GRANT SELECT, INSERT ON ...audit_log 만 나와야 함
--   INSERT 테스트 후 UPDATE 시도 → 45000 에러로 거부되면 정상
