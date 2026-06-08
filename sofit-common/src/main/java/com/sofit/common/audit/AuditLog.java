package com.sofit.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로그(접근기록) 자동 기록 대상 메서드 표시.
 *
 * <p>전자금융감독규정 접근기록 요건을 코드가 아닌 횡단 관심사(AOP)로 자동 충족한다.
 * 이 어노테이션이 붙은 메서드는 {@link AuditLogAspect}가 가로채
 * 성공/실패와 무관하게 audit_log 테이블에 append-only 로 기록한다.</p>
 *
 * <pre>{@code
 * @AuditLog(action = "LOAN_APPROVE", target = "대출 승인")
 * public void approve(Long loanId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 수행한 행위 코드 (예: LOAN_APPROVE, SCB_EVALUATION_VIEW). */
    String action();

    /** 행위 대상에 대한 설명/식별 (선택). 민감정보·원본 인자값을 넣지 말 것. */
    String target() default "";
}
