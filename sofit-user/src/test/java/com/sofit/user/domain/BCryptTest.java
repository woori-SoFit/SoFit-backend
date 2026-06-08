package com.sofit.user.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTest {

    @Test
    void makeHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("123456");

        System.out.println(hash);
        System.out.println(encoder.matches("123456", hash));
    }
}
