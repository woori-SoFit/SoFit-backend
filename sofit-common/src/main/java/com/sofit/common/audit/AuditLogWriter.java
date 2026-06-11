package com.sofit.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 감사 로그 append-only 기록기.
 *
 * <p>메인 {@link JdbcTemplate}(sofit 계정)으로 INSERT만 수행한다.
 * UPDATE/DELETE는 DB 트리거(trg_audit_no_update / trg_audit_no_delete)가 SQLSTATE 45000으로 거부하여
 * append-only를 강제한다. (scripts/sql/audit_log.sql 참고)</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogWriter {

    private static final String INSERT_SQL = """
            INSERT INTO audit_log
                (event_time, actor, actor_role, action, target,
                 source_system, access_method, client_ip, result, trace_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void write(AuditEvent e) {
        jdbcTemplate.update(INSERT_SQL,
                e.eventTime(), e.actor(), e.actorRole(), e.action(), e.target(),
                e.sourceSystem(), e.accessMethod(), e.clientIp(), e.result(), e.traceId());
    }
}
