package com.sofit.user.global.util;

/**
 * 개인정보 마스킹 및 포맷팅 유틸리티
 */
public class MaskingUtil {

    private MaskingUtil() {}

    /**
     * 주민번호 마스킹 처리
     * - null/빈 문자열 → 빈 문자열 반환
     * - 1~6자리 → 원본 + "-" + "******" 반환
     * - 7자리 이상 → 앞 6자리 + "-" + 7번째 자리 + "******" 형식 (총 14자)
     */
    public static String maskResidentNumber(String residentNumber) {
        if (residentNumber == null || residentNumber.isEmpty()) {
            return "";
        }

        if (residentNumber.length() < 7) {
            return residentNumber + "-" + "******";
        }

        return residentNumber.substring(0, 6) + "-" + residentNumber.charAt(6) + "******";
    }

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
            return phoneNumber.substring(0, 3) + "-" + phoneNumber.substring(3, 7) + "-" + phoneNumber.substring(7);
        }

        return phoneNumber;
    }
}
