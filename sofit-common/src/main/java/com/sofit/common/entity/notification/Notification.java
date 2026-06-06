package com.sofit.common.entity.notification;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.common.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_label", length = 100)
    private String referenceLabel;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    public Notification(User user, NotificationType type, Long referenceId, String referenceLabel) {
        this.user = user;
        this.type = type;
        this.title = type.getTitle();
        this.message = type.getMessage();
        this.referenceId = referenceId;
        this.referenceLabel = referenceLabel;
        this.isRead = false;
    }

    public void markAsRead(LocalDateTime readAt) {
        this.isRead = true;
        this.readAt = readAt;
    }
}
