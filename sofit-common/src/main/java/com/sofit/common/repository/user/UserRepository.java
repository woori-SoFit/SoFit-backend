package com.sofit.common.repository.user;

import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<User> findByRoleAndStatus(UserRole role, UserStatus status);

    long countByStatus(UserStatus status);

    long countByStatusAndRole(UserStatus status, UserRole role);

    long countByStatusAndRoleIn(UserStatus status, List<UserRole> roles);
}
