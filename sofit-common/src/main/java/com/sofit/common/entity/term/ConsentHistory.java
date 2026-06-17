package com.sofit.common.entity.term;

import java.time.LocalDateTime;

import com.sofit.common.entity.BaseEntity;
import org.slf4j.MDC;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consent_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ConsentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long consentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private LoanApplication application;

    @Column(name = "is_consented", nullable = false)
    private Boolean isConsented;

    @CreatedDate
    @Column(name = "consented_at", nullable = false, updatable = false)
    private LocalDateTime consentedAt;

    @Column(name = "trace_id", length = 64, updatable = false)
    private String traceId;

    @PrePersist
    void fillTrace() {
        if (this.traceId == null) {
            this.traceId = MDC.get("traceId");
        }
    }

    @Builder
    public ConsentHistory(User user, Term term, LoanApplication application, Boolean isConsented) {
        this.user = user;
        this.term = term;
        this.application = application;
        this.isConsented = isConsented;
    }
}
