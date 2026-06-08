package com.sofit.common.entity.sGrade;

import com.sofit.common.entity.sGrade.enums.BatchStatus;
import com.sofit.common.entity.sGrade.enums.ExecutionCycle;
import com.sofit.common.entity.sGrade.enums.ExecutionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_execution_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false)
    private ExecutionType executionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_cycle", nullable = false)
    private ExecutionCycle executionCycle;

    @Column(name = "triggered_by")
    private Long triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", columnDefinition = "INT DEFAULT 0")
    private Integer successCount;

    @Column(name = "fail_count", columnDefinition = "INT DEFAULT 0")
    private Integer failCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
