package com.sofit.common.audit;

import com.sofit.common.entity.audit.AuditLogEntity;
import com.sofit.common.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 append-only 기록기.
 *
 * <p>{@link AuditLogRepository}(sofit 계정)로 INSERT만 수행한다.
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

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent e) {
        auditLogRepository.save(AuditLogEntity.from(e));
    }
}
