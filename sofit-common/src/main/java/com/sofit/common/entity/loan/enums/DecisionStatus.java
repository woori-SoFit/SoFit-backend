package com.sofit.common.entity.loan.enums;

public enum DecisionStatus {
    SYSTEM_APPROVED,
    SYSTEM_REJECTED,
    TELLER_APPROVED,
    TELLER_REJECTED,
    MANAGER_APPROVED,
    MANAGER_REJECTED;

    /**
     * 시스템 자동 판단 여부
     */
    public boolean isSystem() {
        return this == SYSTEM_APPROVED || this == SYSTEM_REJECTED;
    }

    /**
     * 심사 주체 역할명 반환
     */
    public String getReviewerRole() {
        return switch (this) {
            case SYSTEM_APPROVED, SYSTEM_REJECTED -> "SYSTEM";
            case TELLER_APPROVED, TELLER_REJECTED -> "ADMIN_BANK_TELLER";
            case MANAGER_APPROVED, MANAGER_REJECTED -> "ADMIN_BANK_MANAGER";
        };
    }

    public boolean isApproved() {
        return this == SYSTEM_APPROVED || this == TELLER_APPROVED || this == MANAGER_APPROVED;
    }

    public boolean isRejected() {
        return this == SYSTEM_REJECTED || this == TELLER_REJECTED || this == MANAGER_REJECTED;
    }
}
