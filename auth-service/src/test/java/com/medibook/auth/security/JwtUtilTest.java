package com.medibook.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 512-bit secret (64 chars) required for HS256 with jjwt 0.11.5
    private static final String SECRET =
            "medibook-super-secret-key-for-testing-purposes-only-1234567890ab";
    private static final long EXPIRATION = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", SECRET);
        setField(jwtUtil, "expiration", EXPIRATION);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ── generateToken ─────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("alice@medibook.com", "Patient", 1);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_returnsThreePartJwt() {
        String token = jwtUtil.generateToken("alice@medibook.com", "Patient", 1);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_differentUsers_producesDifferentTokens() {
        String t1 = jwtUtil.generateToken("alice@x.com", "Patient", 1);
        String t2 = jwtUtil.generateToken("bob@x.com", "Provider", 2);
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── extractEmail ─────────────────────────────────────────────────────

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("john@medibook.com", "Admin", 10);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("john@medibook.com");
    }

    @Test
    void extractEmail_roundTrip_variousEmails() {
        String[] emails = {"a@b.com", "doctor@hospital.org", "patient+1@clinic.net"};
        for (String email : emails) {
            String token = jwtUtil.generateToken(email, "Patient", 1);
            assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        }
    }

    // ── extractRole ──────────────────────────────────────────────────────

    @Test
    void extractRole_patient_returnsCorrectRole() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 1);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Patient");
    }

    @Test
    void extractRole_provider_returnsCorrectRole() {
        String token = jwtUtil.generateToken("doc@x.com", "Provider", 2);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Provider");
    }

    @Test
    void extractRole_admin_returnsCorrectRole() {
        String token = jwtUtil.generateToken("admin@x.com", "Admin", 3);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Admin");
    }

    // ── extractUserId ─────────────────────────────────────────────────────

    @Test
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 42);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42);
    }

    @Test
    void extractUserId_largeId_isCorrect() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 99999);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(99999);
    }

    @Test
    void extractUserId_zeroId_isCorrect() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 0);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(0);
    }

    // ── validateToken ─────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 1);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.token")).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@x.com", "Patient", 1);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // Use -1ms expiration to create an already-expired token
        setField(jwtUtil, "expiration", -1L);
        String expiredToken = jwtUtil.generateToken("user@x.com", "Patient", 1);
        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    @Test
    void validateToken_doesNotThrow() {
        assertThatCode(() -> jwtUtil.validateToken("random.garbage.token"))
                .doesNotThrowAnyException();
    }

    // ── Round-trip: generate → extract ───────────────────────────────────

    @Test
    void fullRoundTrip_allClaimsPreserved() {
        String email = "roundtrip@medibook.com";
        String role = "Provider";
        int userId = 777;

        String token = jwtUtil.generateToken(email, role, userId);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(role);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
    }
}
