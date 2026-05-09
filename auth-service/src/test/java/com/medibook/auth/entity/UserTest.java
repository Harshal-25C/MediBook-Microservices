package com.medibook.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    // ── Builder & field mapping ──────────────────────────────────────────

    @Test
    void builder_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(10)
                .fullName("Alice Smith")
                .email("alice@medibook.com")
                .passwordHash("hash123")
                .phone("9876543210")
                .role("Patient")
                .provider("google")
                .isActive(true)
                .createdAt(now)
                .profilePicUrl("http://pic.url/alice.jpg")
                .build();

        assertThat(user.getUserId()).isEqualTo(10);
        assertThat(user.getFullName()).isEqualTo("Alice Smith");
        assertThat(user.getEmail()).isEqualTo("alice@medibook.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash123");
        assertThat(user.getPhone()).isEqualTo("9876543210");
        assertThat(user.getRole()).isEqualTo("Patient");
        assertThat(user.getProvider()).isEqualTo("google");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getProfilePicUrl()).isEqualTo("http://pic.url/alice.jpg");
    }

    @Test
    void noArgsConstructor_createsEmptyUser() {
        User user = new User();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getFullName()).isNull();
        assertThat(user.getUserId()).isEqualTo(0);
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(5, "Bob", "bob@x.com", "hash", "111",
                "Provider", "github", true, now, "pic.jpg");

        assertThat(user.getUserId()).isEqualTo(5);
        assertThat(user.getFullName()).isEqualTo("Bob");
        assertThat(user.getEmail()).isEqualTo("bob@x.com");
        assertThat(user.getRole()).isEqualTo("Provider");
        assertThat(user.getProvider()).isEqualTo("github");
        assertThat(user.isActive()).isTrue();
    }

    // ── Setter / getter round-trip ───────────────────────────────────────

    @Test
    void setters_updateFields() {
        User user = new User();
        user.setFullName("Changed Name");
        user.setEmail("changed@email.com");
        user.setPasswordHash("newHash");
        user.setPhone("000");
        user.setRole("Admin");
        user.setProvider("github");
        user.setActive(false);
        user.setProfilePicUrl("newPic.png");

        assertThat(user.getFullName()).isEqualTo("Changed Name");
        assertThat(user.getEmail()).isEqualTo("changed@email.com");
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.getPhone()).isEqualTo("000");
        assertThat(user.getRole()).isEqualTo("Admin");
        assertThat(user.getProvider()).isEqualTo("github");
        assertThat(user.isActive()).isFalse();
        assertThat(user.getProfilePicUrl()).isEqualTo("newPic.png");
    }

    // ── isActive toggle ──────────────────────────────────────────────────

    @Test
    void isActive_defaultsTrueViaBuilder() {
        User user = User.builder().isActive(true).build();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void isActive_canBeSetFalse() {
        User user = User.builder().isActive(false).build();
        assertThat(user.isActive()).isFalse();
    }

    // ── prePersist ────────────────────────────────────────────────────────

    @Test
    void prePersist_setsCreatedAt() {
        User user = new User();
        assertThat(user.getCreatedAt()).isNull();

        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void prePersist_setsCreatedAtWithinOneSecond() {
        User user = new User();
        LocalDateTime before = LocalDateTime.now();
        user.prePersist();
        LocalDateTime after = LocalDateTime.now();

        assertThat(user.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(after);
    }

    // ── Lombok equals / hashCode ─────────────────────────────────────────

    @Test
    void equalUsers_areEqual() {
        User u1 = User.builder().userId(1).email("a@a.com").fullName("A")
                .role("Patient").isActive(true).build();
        User u2 = User.builder().userId(1).email("a@a.com").fullName("A")
                .role("Patient").isActive(true).build();
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void differentUsers_areNotEqual() {
        User u1 = User.builder().userId(1).email("a@a.com").build();
        User u2 = User.builder().userId(2).email("b@b.com").build();
        assertThat(u1).isNotEqualTo(u2);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    void toString_containsEmail() {
        User user = User.builder().email("test@medibook.com").userId(3).build();
        assertThat(user.toString()).contains("test@medibook.com");
    }

    // ── Roles ────────────────────────────────────────────────────────────

    @Test
    void rolePatient_isSet() {
        User user = User.builder().role("Patient").build();
        assertThat(user.getRole()).isEqualTo("Patient");
    }

    @Test
    void roleProvider_isSet() {
        User user = User.builder().role("Provider").build();
        assertThat(user.getRole()).isEqualTo("Provider");
    }

    @Test
    void roleAdmin_isSet() {
        User user = User.builder().role("Admin").build();
        assertThat(user.getRole()).isEqualTo("Admin");
    }

    // ── Provider (OAuth) ─────────────────────────────────────────────────

    @Test
    void provider_nullForLocalUsers() {
        User user = User.builder().provider(null).build();
        assertThat(user.getProvider()).isNull();
    }

    @Test
    void provider_googleForOAuthUsers() {
        User user = User.builder().provider("google").build();
        assertThat(user.getProvider()).isEqualTo("google");
    }
}
