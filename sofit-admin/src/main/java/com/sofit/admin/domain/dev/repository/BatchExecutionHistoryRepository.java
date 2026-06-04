package com.sofit.admin.domain.dev.repository;

import com.sofit.admin.domain.dev.entity.BatchExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionHistoryRepository extends JpaRepository<BatchExecutionHistory, Long> {
}
