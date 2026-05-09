package com.medibook.auth.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    // ── Constructors ─────────────────────────────────────────────────────

    @Test
    void allArgsConstructor_setsAllFields() {
        AuthResponse response = new AuthResponse("jwt-token", "Patient", 42, "John Doe", "Login successful");

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("Patient");
        assertThat(response.getUserId()).isEqualTo(42);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    void noArgsConstructor_createsEmptyObject() {
        AuthResponse response = new AuthResponse();
        assertThat(response.getToken()).isNull();
        assertThat(response.getRole()).isNull();
        assertThat(response.getUserId()).isEqualTo(0);
        assertThat(response.getFullName()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    // ── Setters ──────────────────────────────────────────────────────────

    @Test
    void setters_updateAllFields() {
        AuthResponse response = new AuthResponse();
        response.setToken("new-token");
        response.setRole("Admin");
        response.setUserId(99);
        response.setFullName("Admin User");
        response.setMessage("Welcome Admin");

        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getRole()).isEqualTo("Admin");
        assertThat(response.getUserId()).isEqualTo(99);
        assertThat(response.getFullName()).isEqualTo("Admin User");
        assertThat(response.getMessage()).isEqualTo("Welcome Admin");
    }

    // ── Roles ────────────────────────────────────────────────────────────

    @Test
    void rolePatient_isSet() {
        AuthResponse response = new AuthResponse("tok", "Patient", 1, "P User", "ok");
        assertThat(response.getRole()).isEqualTo("Patient");
    }

    @Test
    void roleProvider_isSet() {
        AuthResponse response = new AuthResponse("tok", "Provider", 2, "Dr. Smith", "ok");
        assertThat(response.getRole()).isEqualTo("Provider");
    }

    @Test
    void roleAdmin_isSet() {
        AuthResponse response = new AuthResponse("tok", "Admin", 3, "Admin", "ok");
        assertThat(response.getRole()).isEqualTo("Admin");
    }

    // ── Lombok equals / hashCode ─────────────────────────────────────────

    @Test
    void equalResponses_areEqual() {
        AuthResponse r1 = new AuthResponse("tok", "Patient", 1, "John", "ok");
        AuthResponse r2 = new AuthResponse("tok", "Patient", 1, "John", "ok");
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void differentResponses_areNotEqual() {
        AuthResponse r1 = new AuthResponse("tok1", "Patient", 1, "John", "ok");
        AuthResponse r2 = new AuthResponse("tok2", "Provider", 2, "Jane", "ok");
        assertThat(r1).isNotEqualTo(r2);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    void toString_containsToken() {
        AuthResponse response = new AuthResponse("my-jwt-token", "Patient", 1, "User", "msg");
        assertThat(response.toString()).contains("my-jwt-token");
    }

    @Test
    void toString_containsRole() {
        AuthResponse response = new AuthResponse("tok", "Provider", 5, "Doc", "msg");
        assertThat(response.toString()).contains("Provider");
    }

    // ── userId edge cases ─────────────────────────────────────────────────

    @Test
    void userId_canBeZero() {
        AuthResponse response = new AuthResponse("tok", "Patient", 0, "User", "msg");
        assertThat(response.getUserId()).isEqualTo(0);
    }

    @Test
    void userId_canBeLargeNumber() {
        AuthResponse response = new AuthResponse("tok", "Admin", 999999, "Admin", "msg");
        assertThat(response.getUserId()).isEqualTo(999999);
    }
}

