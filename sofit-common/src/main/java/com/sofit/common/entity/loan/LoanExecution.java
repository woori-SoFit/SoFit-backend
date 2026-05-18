package com.sofit.common.entity.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan_execution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Column(name = "execution_amount", nullable = false)
    private Long executionAmount;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;
}
