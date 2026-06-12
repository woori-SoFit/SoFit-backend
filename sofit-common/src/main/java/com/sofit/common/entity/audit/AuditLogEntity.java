package com.sofit.common.entity.audit;

import com.sofit.common.audit.AuditEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감사 로그 테이블 매핑 엔티티.
 *
 * <p>BaseEntity/TraceableEntity 를 상속하지 않는다 — audit_log 는 생성자·수정자 컬럼이 없고
 * trace_id 를 AuditEvent 값으로 직접 채우므로 @PrePersist 자동 주입이 불필요하다.</p>
 *
 * <p>UPDATE/DELETE 는 DB 트리거(trg_audit_no_update / trg_audit_no_delete)가 거부한다.
 * (scripts/sql/audit_log.sql 참고)</p>
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target", length = 200)
    private String target;

    @Column(name = "source_system", nullable = false, length = 20)
    private String sourceSystem;

    @Column(name = "access_method", length = 20)
    private String accessMethod;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Builder
    private AuditLogEntity(LocalDateTime eventTime, String actor, String actorRole,
                           String action, String target, String sourceSystem,
                           String accessMethod, String clientIp, String result, String traceId) {
        this.eventTime = eventTime;
        this.actor = actor;
        this.actorRole = actorRole;
        this.action = action;
        this.target = target;
        this.sourceSystem = sourceSystem;
        this.accessMethod = accessMethod;
        this.clientIp = clientIp;
        this.result = result;
        this.traceId = traceId;
    }

    public static AuditLogEntity from(AuditEvent e) {
        return AuditLogEntity.builder()
                .eventTime(e.eventTime())
                .actor(e.actor())
                .actorRole(e.actorRole())
                .action(e.action())
                .target(e.target())
                .sourceSystem(e.sourceSystem())
                .accessMethod(e.accessMethod())
                .clientIp(e.clientIp())
                .result(e.result())
                .traceId(e.traceId())
                .build();
    }
}
