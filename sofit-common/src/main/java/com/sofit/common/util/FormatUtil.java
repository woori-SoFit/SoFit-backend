package com.sofit.common.util;

/**
 * 데이터 포맷 변환 유틸리티
 */
public class FormatUtil {

    private FormatUtil() {}

    /**
     * 전화번호 포맷팅 (하이픈 삽입)
     * - null/빈 문자열 → 빈 문자열 반환
     * - 11자리 → NNN-NNNN-NNNN 형식
     * - 그 외 → 원본 그대로 반환
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }

        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "-"
                    + phoneNumber.substring(3, 7) + "-"
                    + phoneNumber.substring(7);
        }

        return phoneNumber;
    }
}
