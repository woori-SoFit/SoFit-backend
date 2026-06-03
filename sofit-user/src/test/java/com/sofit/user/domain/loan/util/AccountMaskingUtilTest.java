package com.sofit.user.domain.loan.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

class AccountMaskingUtilTest {

    @ParameterizedTest
    @CsvSource({
            "1234567890, 1234-****-90",
            "123456789, 1234-****-9",
            "110123456789012, 1101-****-6789012"
    })
    @DisplayName("9자리 이상 계좌번호는 '앞4자리-****-9번째부터' 형식으로 마스킹된다")
    void mask_validAccountNumber_returnsMaskedFormat(String input, String expected) {
        // when
        String result = AccountMaskingUtil.mask(input);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("정확히 9자리면 suffix는 마지막 한 자리만 남는다")
    void mask_exactlyMinLength_keepsLastDigit() {
        // when
        String result = AccountMaskingUtil.mask("123456789");

        // then
        assertThat(result).isEqualTo("1234-****-9");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "1234", ""})
    @DisplayName("9자리 미만 계좌번호는 ACCOUNT_INVALID 예외를 던진다")
    void mask_tooShort_throwsAccountInvalid(String input) {
        // when & then
        assertThatThrownBy(() -> AccountMaskingUtil.mask(input))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_INVALID);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null 계좌번호는 ACCOUNT_INVALID 예외를 던진다")
    void mask_null_throwsAccountInvalid(String input) {
        // when & then
        assertThatThrownBy(() -> AccountMaskingUtil.mask(input))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_INVALID);
    }
}
