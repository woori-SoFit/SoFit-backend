package com.sofit.user.domain.notification.event;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanExecutedEvent {

    private final User user;
    private final LoanApplication application;
}
