
package com.medibook.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.medibook.admin.security.JwtUtil;

class GeneratedJwtUtilCoverageTest {
    @Test
    void tokenRoundTripAndInvalidTokenAreCovered() throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        set(jwtUtil, "secret", "0123456789012345678901234567890123456789012345678901234567890123");
        set(jwtUtil, "expiration", 3_600_000L);

        String token = jwtUtil.generateToken("doctor@medibook.com", "Admin", 42);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("doctor@medibook.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Admin");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42);
        assertThat(jwtUtil.validateToken("not-a-token")).isFalse();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
