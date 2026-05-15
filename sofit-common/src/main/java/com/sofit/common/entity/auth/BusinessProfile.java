package com.sofit.common.entity.auth;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_profile_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;

    @Column(name = "representative_name", length = 50)
    private String representativeName;

    @Column(name = "business_category", length = 50)
    private String businessCategory;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "business_name", length = 50)
    private String businessName;

    @Column(name = "business_address", length = 200)
    private String businessAddress;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusinessProfileStatus status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_mybiz_connected", nullable = false)
    private boolean isMybizConnected = false;

    @Column(name = "mybiz_connected_at")
    private LocalDateTime mybizConnectedAt;

    @Column(name = "mydata_all_agreed", nullable = false)
    private boolean mydataAllAgreed = false;

    @Column(name = "mydata_all_agreed_at")
    private LocalDateTime mydataAllAgreedAt;

    // 정적 팩토리 메서드 — KYC 인증 성공 시 사용
    public static BusinessProfile createVerified(User user, String businessNumber,
                                                  String representativeName, String businessCategory,
                                                  String businessType, String businessName,
                                                  String businessAddress, LocalDate openDate) {
        BusinessProfile profile = new BusinessProfile();
        profile.user = user;
        profile.businessNumber = businessNumber;
        profile.representativeName = representativeName;
        profile.businessCategory = businessCategory;
        profile.businessType = businessType;
        profile.businessName = businessName;
        profile.businessAddress = businessAddress;
        profile.openDate = openDate;
        profile.status = BusinessProfileStatus.VERIFIED;
        profile.verifiedAt = LocalDateTime.now();
        return profile;
    }
}
