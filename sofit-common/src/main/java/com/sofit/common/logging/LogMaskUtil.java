package com.sofit.common.logging;

/**
 * 로그 출력 시 PII(개인식별정보) 마스킹 유틸.
 *
 * <p>로그인ID(이메일/아이디), 이름 등 식별 가능한 값은 로그에 그대로 남기지 않는다.</p>
 */
public final class LogMaskUtil {

    private LogMaskUtil() {}

    /**
     * loginId 마스킹 — 앞 3자리만 노출, 나머지 ***
     * <pre>
     *   "abc@bank.com" → "abc***"
     *   "ab"           → "**"
     *   "a"            → "*"
     * </pre>
     */
    public static String maskLoginId(String loginId) {
        return mask(loginId, 3);
    }

    /**
     * 이름 마스킹 — 첫 글자만 노출, 나머지 *
     * <pre>
     *   "홍길동" → "홍**"
     * </pre>
     */
    public static String maskName(String name) {
        return mask(name, 1);
    }

    private static String mask(String value, int visibleChars) {
        if (value == null || value.isBlank()) {
            return "(unknown)";
        }
        if (value.length() <= visibleChars) {
            return "*".repeat(value.length());
        }
        return value.substring(0, visibleChars) + "***";
    }
}
