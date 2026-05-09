package com.medibook.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

// ─────────────────────────────────────────────────────────────────────────────
// SecurityConfig — unit-level (bean creation, password encoder)
// ─────────────────────────────────────────────────────────────────────────────
class SecurityConfigTest {

    /**
     * SecurityConfig.passwordEncoder() must return a BCryptPasswordEncoder.
     * We test the bean method directly — no Spring context needed.
     */
    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesAndMatchesPassword() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "mySecurePassword";
        String encoded = encoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void passwordEncoder_differentPasswordsProduceDifferentHashes() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String hash1 = encoder.encode("pass1");
        String hash2 = encoder.encode("pass2");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void passwordEncoder_wrongPasswordDoesNotMatch() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("correctPassword");
        assertThat(encoder.matches("wrongPassword", hash)).isFalse();
    }

    @Test
    void passwordEncoder_samePasswordEncodedTwiceProducesDifferentHashes() {
        // BCrypt uses salts — same plain text → different hashes each time
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String hash1 = encoder.encode("samePass");
        String hash2 = encoder.encode("samePass");

        assertThat(hash1).isNotEqualTo(hash2);
        // But both should match the original
        assertThat(encoder.matches("samePass", hash1)).isTrue();
        assertThat(encoder.matches("samePass", hash2)).isTrue();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AuthResponse (security package)
// ─────────────────────────────────────────────────────────────────────────────
class SecurityAuthResponseTest {

    @Test
    void allArgsConstructor_setsFields() {
        AuthResponse response = new AuthResponse("token123", "Patient", 5, "John Doe", "Login successful");
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getRole()).isEqualTo("Patient");
        assertThat(response.getUserId()).isEqualTo(5);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    void noArgsConstructor_createsEmpty() {
        AuthResponse response = new AuthResponse();
        assertThat(response.getToken()).isNull();
        assertThat(response.getFullName()).isNull();
        assertThat(response.getUserId()).isEqualTo(0);
    }

    @Test
    void setters_updateAllFields() {
        AuthResponse response = new AuthResponse();
        response.setToken("new-jwt");
        response.setRole("Admin");
        response.setUserId(99);
        response.setFullName("Admin Name");
        response.setMessage("Welcome");

        assertThat(response.getToken()).isEqualTo("new-jwt");
        assertThat(response.getRole()).isEqualTo("Admin");
        assertThat(response.getUserId()).isEqualTo(99);
        assertThat(response.getFullName()).isEqualTo("Admin Name");
        assertThat(response.getMessage()).isEqualTo("Welcome");
    }

    @Test
    void equalResponses_areEqual() {
        AuthResponse r1 = new AuthResponse("tok", "Patient", 1, "User", "ok");
        AuthResponse r2 = new AuthResponse("tok", "Patient", 1, "User", "ok");
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void differentResponses_areNotEqual() {
        AuthResponse r1 = new AuthResponse("tok1", "Patient", 1, "User1", "ok");
        AuthResponse r2 = new AuthResponse("tok2", "Provider", 2, "User2", "ok");
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void toString_containsFieldValues() {
        AuthResponse response = new AuthResponse("mytoken", "Admin", 10, "Admin User", "msg");
        String str = response.toString();
        assertThat(str).contains("mytoken");
        assertThat(str).contains("Admin");
    }
}
