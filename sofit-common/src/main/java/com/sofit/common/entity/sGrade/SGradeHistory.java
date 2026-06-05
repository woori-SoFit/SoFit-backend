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
}
