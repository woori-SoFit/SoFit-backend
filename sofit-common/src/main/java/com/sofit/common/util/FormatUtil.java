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

    /**
     * 사업자등록번호 포맷팅 (하이픈 삽입)
     * - null/빈 문자열 → 빈 문자열 반환
     * - 10자리 → NNN-NN-NNNNN 형식
     * - 그 외 → 원본 그대로 반환
     */
    public static String formatBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.isEmpty()) {
            return "";
        }

        if (businessNumber.length() == 10) {
            return businessNumber.substring(0, 3) + "-"
                    + businessNumber.substring(3, 5) + "-"
                    + businessNumber.substring(5);
        }

        return businessNumber;
    }
}
