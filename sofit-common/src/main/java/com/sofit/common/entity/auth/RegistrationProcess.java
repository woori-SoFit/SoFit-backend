package com.sofit.common.entity.auth;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원가입 멀티스텝 플로우를 추적하는 엔티티.
 * 가입 완료 시 KYC 데이터를 기반으로 BusinessProfile이 별도 생성된다.
 */
@Entity
@Table(name = "registration_process")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistrationProcess extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_process_id")
    private Long id;

    // 회원가입 프로세스 임시 식별자 (완료 후 null로 설정)
    @Column(name = "registration_id", length = 36, unique = true)
    private String registrationId;

    // 가입 완료 후 연결되는 User (가입 전에는 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 회원가입 단계 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false)
    private RegistrationStep step;

    // KYC 인증 결과 (사업자 정보)
    @Column(name = "business_number", length = 10)
    private String businessNumber;

    @Column(name = "business_name", length = 50)
    private String businessName;

    @Column(name = "representative_name", length = 50)
    private String representativeName;

    @Column(name = "open_date", length = 10)
    private String openDate;

    @Column(name = "business_type", length = 50)
    private String businessType;

    // PIN 인증 결과
    @Column(name = "pin_verified")
    private Boolean pinVerified;

    @Column(name = "pin_verified_at")
    private LocalDateTime pinVerifiedAt;

    /**
     * 팩토리 메서드: Step 1 완료 시 생성
     * KYC 인증 성공 후 사업자 정보를 저장하고 step=STEP_1_COMPLETED로 설정
     */
    public static RegistrationProcess createForStep1(String registrationId,
                                                      String businessNumber,
                                                      String businessName,
                                                      String representativeName,
                                                      String openDate,
                                                      String businessType) {
        RegistrationProcess process = new RegistrationProcess();
        process.registrationId = registrationId;
        process.businessNumber = businessNumber;
        process.businessName = businessName;
        process.representativeName = representativeName;
        process.openDate = openDate;
        process.businessType = businessType;
        process.step = RegistrationStep.STEP_1_COMPLETED;
        process.pinVerified = false;
        return process;
    }

    /**
     * Step 2 완료 처리
     * PIN 인증 성공 시 pinVerified=true, pinVerifiedAt 설정, step=STEP_2_COMPLETED
     */
    public void completeStep2() {
        this.pinVerified = true;
        this.pinVerifiedAt = LocalDateTime.now();
        this.step = RegistrationStep.STEP_2_COMPLETED;
    }

    /**
     * 가입 완료 처리
     * User 연결, registrationId null 설정, step=COMPLETED
     */
    public void completeRegistration(User user) {
        this.user = user;
        this.registrationId = null;
        this.step = RegistrationStep.COMPLETED;
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.step = RegistrationStep.EXPIRED;
    }

    /**
     * 만료 여부 확인 (created_at + 30분 경과 여부)
     */
    public boolean isExpired() {
        return getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now());
    }
}
