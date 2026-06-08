package com.sofit.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.slf4j.MDC;

/**
 * 서버 경계(DB)에서 추적 흔적을 남기는 엔티티 베이스.
 *
 * <p>user / admin / batch 는 서로 호출 관계가 없고 DB만 공유한다. 따라서 서버 간 traceId 전파가
 * 불가능하다. 대신 공유 테이블에 <b>어느 서버가(source_system) 어떤 요청에서(trace_id)</b>
 * INSERT 했는지를 남겨, 행(row)이 추적 접점이 되게 한다.</p>
 *
 * <p>활용: {@code loan_decision} 이상 징후 발견 시 → {@code source_system} 으로 작성 서버 식별 →
 * {@code trace_id}(로그 JSON의 traceId 와 동일) 로 해당 서버 로그를 grep = "사실상의 분산 추적".</p>
 *
 * <p>값은 {@link PrePersist} 시점에 MDC(TraceIdFilter / 배치 스케줄러가 주입)에서 자동 채운다.
 * INSERT 전용(updatable=false) — 최초 작성 흔적은 이후 변경되지 않는다.</p>
 */
@Getter
@MappedSuperclass
public abstract class TraceableEntity extends BaseEntity {

    @Column(name = "source_system", length = 20, updatable = false)
    private String sourceSystem;   // USER / ADMIN / BATCH

    @Column(name = "trace_id", length = 64, updatable = false)
    private String traceId;        // 작성 요청의 traceId (로그 JSON의 traceId 와 동일 값)

    @PrePersist
    void fillTrace() {
        if (this.sourceSystem == null) {
            this.sourceSystem = MDC.get("sourceSystem");
        }
        if (this.traceId == null) {
            this.traceId = MDC.get("traceId");
        }
    }
}
