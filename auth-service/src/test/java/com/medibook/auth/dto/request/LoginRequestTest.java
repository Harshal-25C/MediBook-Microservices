package com.medibook.auth.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ─────────────────────────────────────────────────────────────────────────────
// LoginRequest
// ─────────────────────────────────────────────────────────────────────────────
class LoginRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@medibook.com");
        req.setPassword("securePass");

        assertThat(req.getEmail()).isEqualTo("user@medibook.com");
        assertThat(req.getPassword()).isEqualTo("securePass");
    }

    @Test
    void noArgsConstructor_createsEmptyObject() {
        LoginRequest req = new LoginRequest();
        assertThat(req.getEmail()).isNull();
        assertThat(req.getPassword()).isNull();
    }

    @Test
    void equalRequests_areEqual() {
        LoginRequest r1 = new LoginRequest();
        r1.setEmail("a@b.com"); r1.setPassword("pass");

        LoginRequest r2 = new LoginRequest();
        r2.setEmail("a@b.com"); r2.setPassword("pass");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void differentRequests_areNotEqual() {
        LoginRequest r1 = new LoginRequest();
        r1.setEmail("a@b.com");

        LoginRequest r2 = new LoginRequest();
        r2.setEmail("x@y.com");

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void toString_containsEmail() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@email.com");
        assertThat(req.toString()).contains("test@email.com");
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RegisterRequest
// ─────────────────────────────────────────────────────────────────────────────
class RegisterRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("John Doe");
        req.setEmail("john@medibook.com");
        req.setPassword("pass123");
        req.setPhone("9876543210");
        req.setRole("Patient");

        assertThat(req.getFullName()).isEqualTo("John Doe");
        assertThat(req.getEmail()).isEqualTo("john@medibook.com");
        assertThat(req.getPassword()).isEqualTo("pass123");
        assertThat(req.getPhone()).isEqualTo("9876543210");
        assertThat(req.getRole()).isEqualTo("Patient");
    }

    @Test
    void noArgsConstructor_createsEmptyObject() {
        RegisterRequest req = new RegisterRequest();
        assertThat(req.getFullName()).isNull();
        assertThat(req.getRole()).isNull();
    }

    @Test
    void roleProvider_isAllowed() {
        RegisterRequest req = new RegisterRequest();
        req.setRole("Provider");
        assertThat(req.getRole()).isEqualTo("Provider");
    }

    @Test
    void equalRequests_areEqual() {
        RegisterRequest r1 = new RegisterRequest();
        r1.setEmail("a@b.com"); r1.setRole("Patient"); r1.setPassword("p");

        RegisterRequest r2 = new RegisterRequest();
        r2.setEmail("a@b.com"); r2.setRole("Patient"); r2.setPassword("p");

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void differentRequests_areNotEqual() {
        RegisterRequest r1 = new RegisterRequest(); r1.setEmail("a@b.com");
        RegisterRequest r2 = new RegisterRequest(); r2.setEmail("x@y.com");
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void toString_containsEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("reg@test.com");
        assertThat(req.toString()).contains("reg@test.com");
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RegisterAdminRequest
// ─────────────────────────────────────────────────────────────────────────────
class RegisterAdminRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("Admin User");
        req.setEmail("admin@medibook.com");
        req.setPassword("adminPass");
        req.setAdminCode("SECRET_CODE");

        assertThat(req.getFullName()).isEqualTo("Admin User");
        assertThat(req.getEmail()).isEqualTo("admin@medibook.com");
        assertThat(req.getPassword()).isEqualTo("adminPass");
        assertThat(req.getAdminCode()).isEqualTo("SECRET_CODE");
    }

    @Test
    void noArgsConstructor_createsEmptyObject() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        assertThat(req.getFullName()).isNull();
        assertThat(req.getAdminCode()).isNull();
    }

    @Test
    void adminCode_canBeSetAndRead() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setAdminCode("MEDIBOOK_ADMIN_2024");
        assertThat(req.getAdminCode()).isEqualTo("MEDIBOOK_ADMIN_2024");
    }

    @Test
    void email_canBeSetAndRead() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setEmail("superadmin@hospital.com");
        assertThat(req.getEmail()).isEqualTo("superadmin@hospital.com");
    }

    @Test
    void password_canBeSetAndRead() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setPassword("strongP@ss123");
        assertThat(req.getPassword()).isEqualTo("strongP@ss123");
    }

    @Test
    void fullName_canBeSetAndRead() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("Dr. Admin");
        assertThat(req.getFullName()).isEqualTo("Dr. Admin");
    }
}
