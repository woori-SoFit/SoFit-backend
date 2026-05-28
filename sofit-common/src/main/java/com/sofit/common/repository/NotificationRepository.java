package com.sofit.common.repository;

import com.sofit.common.entity.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop100ByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
