package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

@ExtendWith(MockitoExtension.class)
class BankerAssignmentServiceImplTest {

    @InjectMocks
    private BankerAssignmentServiceImpl bankerAssignmentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String ROUND_ROBIN_KEY = "banker:assign:index";

    @Test
    @DisplayName("assignBanker - 라운드로빈으로 은행원을 배정한다")
    void assignBanker_success_roundRobin() {
        // given
        User banker1 = createBanker(10L);
        User banker2 = createBanker(20L);
        User banker3 = createBanker(30L);

        given(userRepository.findByRoleAndStatus(UserRole.ADMIN_BANK_TELLER, UserStatus.ACTIVE))
                .willReturn(List.of(banker1, banker2, banker3));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(ROUND_ROBIN_KEY)).willReturn(1L);

        // when
        Long assignedBankerId = bankerAssignmentService.assignBanker();

        // then: index=1, 3명 → 1 % 3 = 1 → banker2 (userId=20)
        assertThat(assignedBankerId).isEqualTo(20L);
    }

    @Test
    @DisplayName("assignBanker - 인덱스 0이면 첫 번째 은행원을 배정한다")
    void assignBanker_assignsFirstBanker_whenIndexZero() {
        // given
        User banker1 = createBanker(10L);
        User banker2 = createBanker(20L);

        given(userRepository.findByRoleAndStatus(UserRole.ADMIN_BANK_TELLER, UserStatus.ACTIVE))
                .willReturn(List.of(banker1, banker2));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(ROUND_ROBIN_KEY)).willReturn(0L);

        // when
        Long assignedBankerId = bankerAssignmentService.assignBanker();

        // then: index=0, 2명 → 0 % 2 = 0 → banker1 (userId=10)
        assertThat(assignedBankerId).isEqualTo(10L);
    }

    @Test
    @DisplayName("assignBanker - 활성 은행원이 없으면 NO_AVAILABLE_BANKER 예외")
    void assignBanker_throwsException_whenNoBankers() {
        // given
        given(userRepository.findByRoleAndStatus(UserRole.ADMIN_BANK_TELLER, UserStatus.ACTIVE))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> bankerAssignmentService.assignBanker())
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.NO_AVAILABLE_BANKER);
                });
    }

    @Test
    @DisplayName("assignBanker - Redis increment가 null이면 NO_AVAILABLE_BANKER 예외")
    void assignBanker_throwsException_whenRedisReturnsNull() {
        // given
        User banker = createBanker(10L);

        given(userRepository.findByRoleAndStatus(UserRole.ADMIN_BANK_TELLER, UserStatus.ACTIVE))
                .willReturn(List.of(banker));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(ROUND_ROBIN_KEY)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> bankerAssignmentService.assignBanker())
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.NO_AVAILABLE_BANKER);
                });
    }

    // --- 헬퍼 메서드 ---

    private User createBanker(Long userId) {
        User user = User.createUser("banker" + userId, "hashedpw", "은행원", "01000000000", "8001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "role", UserRole.ADMIN_BANK_TELLER);
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        return user;
    }
}
