package com.sofit.common.entity.sGrade;

import com.sofit.common.entity.sGrade.enums.SGradeStatus;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "s_grade_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SGradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "s_grade_id")
    private Long sGradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_execution_id")
    private BatchExecutionHistory batchExecutionHistory;

    @Column(name = "feature_id")
    private Long featureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SGradeStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    // === 비즈니스 메서드 ===

    /**
     * 회원가입 완료 시 S등급 산출 요청 레코드를 생성한다.
     * featureId, batchExecutionId는 null (Python 배치가 나중에 채움)
     */
    public static SGradeHistory createRequested(User user) {
        SGradeHistory history = new SGradeHistory();
        history.user = user;
        history.status = SGradeStatus.REQUESTED;
        history.requestedAt = LocalDateTime.now();
        return history;
    }

    /**
     * 수동 배치 트리거 시 FAILED → REQUESTED 상태로 복구한다.
     */
    public void resetToRequested() {
        this.status = SGradeStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * S등급 산출 완료 시 상태를 COMPLETED로 변경한다.
     */
    public void markCompleted(Long featureId) {
        this.status = SGradeStatus.COMPLETED;
        this.featureId = featureId;
        this.evaluatedAt = LocalDateTime.now();
    }

    /**
     * S등급 산출 실패 시 상태를 FAILED로 변경한다.
     */
    public void markFailed() {
        this.status = SGradeStatus.FAILED;
    }
}
