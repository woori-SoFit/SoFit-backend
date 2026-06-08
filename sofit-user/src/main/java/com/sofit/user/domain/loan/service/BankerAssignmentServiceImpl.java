package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankerAssignmentServiceImpl implements BankerAssignmentService {

    private static final String ROUND_ROBIN_KEY = "banker:assign:index";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Long assignBanker() {
        // 1. 활성 은행원 목록 조회
        List<User> activeBankers = new ArrayList<>(userRepository
                .findByRoleAndStatus(UserRole.ADMIN_BANK_TELLER, UserStatus.ACTIVE));

        if (activeBankers.isEmpty()) {
            throw new BaseException(LoanErrorCode.NO_AVAILABLE_BANKER);
        }

        // 2. userId 오름차순 정렬 (결정론적 배정 보장)
        activeBankers.sort(Comparator.comparing(User::getUserId));

        // 3. Redis INCR로 인덱스 원자적 증가
        Long index = redisTemplate.opsForValue().increment(ROUND_ROBIN_KEY);
        if (index == null) {
            throw new BaseException(LoanErrorCode.NO_AVAILABLE_BANKER);
        }

        // 4. modulo 연산으로 배정 대상 결정 (오버플로 시 음수 방지)
        int targetIndex = (int) (Math.abs(index) % activeBankers.size());

        return activeBankers.get(targetIndex).getUserId();
    }
}
