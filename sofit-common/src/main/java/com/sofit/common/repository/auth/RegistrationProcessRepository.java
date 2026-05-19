package com.sofit.common.repository.auth;

import com.sofit.common.entity.auth.RegistrationProcess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationProcessRepository extends JpaRepository<RegistrationProcess, Long> {
}
