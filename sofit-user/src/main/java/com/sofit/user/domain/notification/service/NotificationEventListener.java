package com.sofit.user.domain.notification.service;

import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.user.domain.notification.event.LoanExecutedEvent;
import com.sofit.user.domain.notification.event.LoanSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    // 대출 신청 완료 알림
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoanSubmitted(LoanSubmittedEvent event) {
        notificationService.send(
                event.getUserId(),
                NotificationType.LOAN_SUBMITTED,
                event.getApplicationId()
        );
    }

    // 대출 실행 완료 알림
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoanExecuted(LoanExecutedEvent event) {
        notificationService.send(
                event.getUserId(),
                NotificationType.LOAN_EXECUTED,
                event.getApplicationId()
        );
    }
}
