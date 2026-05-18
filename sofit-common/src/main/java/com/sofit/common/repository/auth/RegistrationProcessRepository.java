package com.sofit.common.repository.auth;

import com.sofit.common.entity.auth.RegistrationProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationProcessRepository extends JpaRepository<RegistrationProcess, Long> {

    Optional<RegistrationProcess> findByRegistrationId(String registrationId);
}
