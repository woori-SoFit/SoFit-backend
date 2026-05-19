package com.sofit.common.entity.auth;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원가입 멀티스텝 플로우를 추적하는 엔티티.
 * 세션에 PK(registration_process_id)를 저장하여 프로세스를 추적한다.
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

    // 회원가입 단계 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, columnDefinition = "VARCHAR(20)")
    private RegistrationStep step;

    // KYC 인증 결과 (사업자 정보)
    @Column(name = "business_number", length = 10, unique = true)
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
    @Column(name = "pin_verified", columnDefinition = "TINYINT(1)")
    private Boolean pinVerified;

    @Column(name = "pin_verified_at")
    private LocalDateTime pinVerifiedAt;

    /**
     * 팩토리 메서드: Step 1 완료 시 생성
     */
    public static RegistrationProcess createForStep1(String businessNumber,
                                                      String businessName,
                                                      String representativeName,
                                                      String openDate,
                                                      String businessType) {
        RegistrationProcess process = new RegistrationProcess();
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
     * KYC 재요청 시 기존 레코드 업데이트
     */
    public void updateKycResult(String businessNumber,
                                 String businessName,
                                 String representativeName,
                                 String openDate,
                                 String businessType) {
        this.businessNumber = businessNumber;
        this.businessName = businessName;
        this.representativeName = representativeName;
        this.openDate = openDate;
        this.businessType = businessType;
        this.step = RegistrationStep.STEP_1_COMPLETED;
        this.pinVerified = false;
        this.pinVerifiedAt = null;
    }

    /**
     * Step 2 완료 처리
     */
    public void completeStep2() {
        this.pinVerified = true;
        this.pinVerifiedAt = LocalDateTime.now();
        this.step = RegistrationStep.STEP_2_COMPLETED;
    }

    /**
     * 가입 완료 처리
     */
    public void completeRegistration() {
        this.step = RegistrationStep.COMPLETED;
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.step = RegistrationStep.EXPIRED;
    }
}
