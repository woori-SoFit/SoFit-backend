package com.sofit.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 append-only 기록기.
 *
 * <p>메인 {@link JdbcTemplate}(sofit 계정)으로 INSERT만 수행한다.
 * UPDATE/DELETE는 DB 트리거(trg_audit_no_update / trg_audit_no_delete)가 SQLSTATE 45000으로 거부하여
 * append-only를 강제한다. (scripts/sql/audit_log.sql 참고)</p>
 *
 * <p>{@code REQUIRES_NEW}: 비즈니스 트랜잭션이 롤백되어도 감사 로그는 독립된 트랜잭션으로 커밋된다.
 * FAILURE 결과도 반드시 기록해야 하는 전자금융감독규정 요건을 만족시키기 위함.</p>
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent e) {
        jdbcTemplate.update(INSERT_SQL,
                e.eventTime(), e.actor(), e.actorRole(), e.action(), e.target(),
                e.sourceSystem(), e.accessMethod(), e.clientIp(), e.result(), e.traceId());
    }
}
