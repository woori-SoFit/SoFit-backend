package com.sofit.user.domain.notification.service;

import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.user.domain.notification.event.LoanSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    // LoanSubmittedEvent가 발행되면 트랜잭션 커밋 후 이 메서드가 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoanSubmitted(LoanSubmittedEvent event) {
        notificationService.send(
                event.getUser(),
                NotificationType.LOAN_SUBMITTED,
                event.getApplication()
        );
    }
}
