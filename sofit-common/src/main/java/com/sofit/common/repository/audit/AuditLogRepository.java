package com.sofit.common.repository.audit;

import com.sofit.common.entity.audit.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 감사 로그 저장소 — INSERT 전용.
 *
 * <p>UPDATE/DELETE 는 DB 트리거가 SQLSTATE 45000으로 거부하므로
 * 리포지토리에서 별도로 막지 않는다.</p>
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
