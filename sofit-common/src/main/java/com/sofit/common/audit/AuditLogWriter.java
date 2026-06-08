package com.sofit.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 감사 로그 append-only 기록기.
 *
 * <p>전용 {@code auditJdbcTemplate}(INSERT/SELECT 권한만 가진 audit_writer 계정)으로
 * INSERT 만 수행한다. UPDATE/DELETE API 자체를 노출하지 않으며, 계정 권한으로도 막혀 있어
 * "쓴 주체도 변조 불가"가 권한 레벨에서 강제된다.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogWriter {

    private static final String INSERT_SQL = """
            INSERT INTO audit_log
                (event_time, actor, actor_role, action, target,
                 source_system, access_method, client_ip, result, request_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Qualifier("auditJdbcTemplate")
    private final JdbcTemplate auditJdbcTemplate;

    public void write(AuditEvent e) {
        auditJdbcTemplate.update(INSERT_SQL,
                e.eventTime(), e.actor(), e.actorRole(), e.action(), e.target(),
                e.sourceSystem(), e.accessMethod(), e.clientIp(), e.result(), e.requestId());
    }
}
