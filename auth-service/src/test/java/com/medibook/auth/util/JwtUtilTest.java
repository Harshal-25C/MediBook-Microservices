package com.medibook.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil — Unit Tests")
class JwtUtilTest {
 
    private JwtUtil jwtUtil;
 
    // Must be at least 256 bits (32 chars) for HS256
    private static final String TEST_SECRET =
            "medibook-test-secret-key-must-be-at-least-256-bits-long-here";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours
 
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION_MS);
    }
 
    // ─── generateToken ───────────────────────────────────────────────────────
 
    @Test
    @DisplayName("generateToken — returns non-null, non-empty token")
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("test@gmail.com", "PATIENT", 1L);
        assertThat(token).isNotNull().isNotBlank();
    }
 
    @Test
    @DisplayName("generateToken — token has 3 JWT parts separated by dots")
    void generateToken_shouldHaveThreeParts() {
        String token = jwtUtil.generateToken("test@gmail.com", "PATIENT", 1L);
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }
 
    @Test
    @DisplayName("generateToken — different emails produce different tokens")
    void generateToken_differentEmailsProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@gmail.com", "PATIENT", 1L);
        String token2 = jwtUtil.generateToken("user2@gmail.com", "PATIENT", 2L);
        assertThat(token1).isNotEqualTo(token2);
    }
 
    // ─── extractEmail ────────────────────────────────────────────────────────
 
    @Test
    @DisplayName("extractEmail — returns correct email from token")
    void extractEmail_shouldReturnCorrectEmail() {
        String email = "rahul@gmail.com";
        String token = jwtUtil.generateToken(email, "PATIENT", 10L);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }
 
    @Test
    @DisplayName("extractEmail — returns correct email for admin role")
    void extractEmail_shouldWorkForAdminRole() {
        String email = "admin@medibook.com";
        String token = jwtUtil.generateToken(email, "ADMIN", 99L);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }
 
    // ─── extractRole ─────────────────────────────────────────────────────────
 
    @Test
    @DisplayName("extractRole — returns PATIENT role from token")
    void extractRole_shouldReturnPatientRole() {
        String token = jwtUtil.generateToken("p@test.com", "PATIENT", 1L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("PATIENT");
    }
 
    @Test
    @DisplayName("extractRole — returns PROVIDER role from token")
    void extractRole_shouldReturnProviderRole() {
        String token = jwtUtil.generateToken("doc@test.com", "PROVIDER", 2L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("PROVIDER");
    }
 
    @Test
    @DisplayName("extractRole — returns ADMIN role from token")
    void extractRole_shouldReturnAdminRole() {
        String token = jwtUtil.generateToken("admin@test.com", "ADMIN", 3L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }
 
    // ─── validateToken ───────────────────────────────────────────────────────
 
    @Test
    @DisplayName("validateToken — returns true for a valid token")
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("valid@test.com", "PATIENT", 1L);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
 
    @Test
    @DisplayName("validateToken — returns false for a tampered token")
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = jwtUtil.generateToken("valid@test.com", "PATIENT", 1L);
        String tampered = token + "tampered";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }
 
    @Test
    @DisplayName("validateToken — returns false for a completely random string")
    void validateToken_shouldReturnFalseForRandomString() {
        assertThat(jwtUtil.validateToken("not.a.jwt.token.at.all")).isFalse();
    }
 
    @Test
    @DisplayName("validateToken — returns false for empty string")
    void validateToken_shouldReturnFalseForEmptyString() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }
 
    @Test
    @DisplayName("validateToken — returns false for null")
    void validateToken_shouldReturnFalseForNull() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }
 
    @Test
    @DisplayName("validateToken — returns false for expired token")
    void validateToken_shouldReturnFalseForExpiredToken() throws InterruptedException {
        // Set expiry to 1 millisecond
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L);
        String token = jwtUtil.generateToken("exp@test.com", "PATIENT", 1L);
        Thread.sleep(10); // Wait for token to expire
        assertThat(jwtUtil.validateToken(token)).isFalse();
        // Restore
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION_MS);
    }
 
    @Test
    @DisplayName("validateToken — returns false for token signed with wrong secret")
    void validateToken_shouldReturnFalseForWrongSecret() {
        // Generate token with a different secret
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "jwtSecret",
                "completely-different-secret-key-at-least-256-bits-long-!!!");
        ReflectionTestUtils.setField(otherJwtUtil, "jwtExpiration", EXPIRATION_MS);
 
        String tokenFromOther = otherJwtUtil.generateToken("user@test.com", "PATIENT", 1L);
        assertThat(jwtUtil.validateToken(tokenFromOther)).isFalse();
    }
 
    // ─── round-trip: generate → extract → validate ───────────────────────────
 
    @Test
    @DisplayName("round-trip — generate, extract email, extract role, validate all consistent")
    void roundTrip_shouldBeFullyConsistent() {
        String email = "roundtrip@test.com";
        String role = "PROVIDER";
        Long userId = 42L;
 
        String token = jwtUtil.generateToken(email, role, userId);
 
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(role);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
}
