package com.sofit.common.repository.sGrade;

import com.sofit.common.entity.sGrade.Scb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScbRepository extends JpaRepository<Scb, Long> {

    Optional<Scb> findByApplicationId(Long applicationId);
}
