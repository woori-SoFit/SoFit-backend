package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.converter.DevUserConverter;
import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.admin.domain.dev.repository.UserSpecification;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevUserServiceImpl implements DevUserService {

    private static final int DEFAULT_SIZE = 8;

    private final UserRepository userRepository;

    @Override
    public UserListResponse findUsers(Integer page, Integer size, String keyword, String role, String status) {
        int actualSize = (size != null) ? size : DEFAULT_SIZE;
        int actualPage = (page != null) ? page : 0;
        int pageIndex = actualPage; // 0-based

        Pageable pageable = PageRequest.of(pageIndex, actualSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        Specification<User> keywordSpec = UserSpecification.keywordContains(keyword);
        if (keywordSpec != null) {
            spec = spec.and(keywordSpec);
        }

        Specification<User> roleSpec = UserSpecification.roleEquals(role);
        if (roleSpec != null) {
            spec = spec.and(roleSpec);
        }

        Specification<User> statusSpec = UserSpecification.statusEquals(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);

        return DevUserConverter.toUserListResponse(userPage, actualPage, actualSize);
    }
}
