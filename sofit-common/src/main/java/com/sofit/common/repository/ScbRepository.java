package com.sofit.common.repository;

import com.sofit.common.entity.report.Scb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScbRepository extends JpaRepository<Scb, Long> {

    Optional<Scb> findByApplicationId(Long applicationId);
}
