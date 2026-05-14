package com.sofit.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
