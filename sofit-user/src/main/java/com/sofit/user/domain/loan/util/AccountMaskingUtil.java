package com.sofit.user.domain.loan.util;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

/**
 * 계좌번호 마스킹 유틸리티
 * 형식: "{앞4자리}-****-{9번째 자리부터 끝}"
 */
public class AccountMaskingUtil {

    private static final int MIN_LENGTH = 9;

    private AccountMaskingUtil() {
    }

    /**
     * 계좌번호를 마스킹 처리한다.
     * 예: "1234567890" → "1234-****-90"
     *
     * @param accountNumber 원본 계좌번호 (숫자만, 9자리 이상)
     * @return 마스킹된 계좌번호
     * @throws BaseException 9자리 미만인 경우 ACCOUNT4001
     */
    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < MIN_LENGTH) {
            throw new BaseException(LoanErrorCode.ACCOUNT_INVALID);
        }

        String prefix = accountNumber.substring(0, 4);
        String suffix = accountNumber.substring(8);

        return prefix + "-****-" + suffix;
    }
}
