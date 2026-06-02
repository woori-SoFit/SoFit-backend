package com.sofit.common.repository.projection;

import com.sofit.common.entity.loan.enums.ApplicationStatus;

/**
 * 대출 신청 상태별 건수 집계를 위한 JPA Projection 인터페이스.
 * GROUP BY 쿼리 결과를 매핑하는 데 사용된다.
 */
public interface StatusCountProjection {

    ApplicationStatus getStatus();

    Long getCount();
}
