package com.sofit.common.entity.term;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "is_consented", nullable = false)
    private Boolean isConsented;

    @CreatedDate
    @Column(name = "consented_at", nullable = false, updatable = false)
    private LocalDateTime consentedAt;

    @Builder
    public ConsentHistory(Long userId, Long termId, Long applicationId, Boolean isConsented) {
        this.userId = userId;
        this.termId = termId;
        this.applicationId = applicationId;
        this.isConsented = isConsented;
    }
}
