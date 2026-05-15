package com.sofit.common.repository.auth;

import com.sofit.common.entity.auth.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
}
