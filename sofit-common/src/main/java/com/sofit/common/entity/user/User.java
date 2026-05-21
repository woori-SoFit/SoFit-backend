package com.sofit.common.entity.user;

import com.sofit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "resident_number", nullable = false, length = 7)
    private String residentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "inactivated_at")
    private LocalDateTime inactivatedAt;

    /**
     * 회원가입 시 사용하는 정적 팩토리 메서드
     * role=USER, status=ACTIVE로 생성
     */
    public static User createUser(String loginId, String passwordHash, String name,
                                   String phoneNumber, String residentNumber) {
        User user = new User();
        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.name = name;
        user.phoneNumber = phoneNumber;
        user.residentNumber = residentNumber;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    /**
     * 회원탈퇴 처리 (Soft Delete)
     * status를 INACTIVE로 변경하고 inactivatedAt을 기록
     */
    public void inactivate() {
        this.status = UserStatus.INACTIVE;
        this.inactivatedAt = LocalDateTime.now();
    }
}
