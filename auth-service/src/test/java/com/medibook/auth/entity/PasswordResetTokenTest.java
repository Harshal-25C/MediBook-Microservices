package com.medibook.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    // ── Builder ──────────────────────────────────────────────────────────

    @Test
    void builder_setsAllFields() {
        LocalDateTime expires = LocalDateTime.now().plusMinutes(15);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1)
                .email("alice@medibook.com")
                .token("abc-uuid-123")
                .otp("654321")
                .expiresAt(expires)
                .used(false)
                .build();

        assertThat(token.getId()).isEqualTo(1);
        assertThat(token.getEmail()).isEqualTo("alice@medibook.com");
        assertThat(token.getToken()).isEqualTo("abc-uuid-123");
        assertThat(token.getOtp()).isEqualTo("654321");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void noArgsConstructor_createsEmptyToken() {
        PasswordResetToken token = new PasswordResetToken();
        assertThat(token.getEmail()).isNull();
        assertThat(token.getToken()).isNull();
        assertThat(token.getOtp()).isNull();
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime expires = LocalDateTime.now().plusMinutes(10);
        PasswordResetToken token = new PasswordResetToken(2, "b@b.com", "tok", "111111", expires, true);

        assertThat(token.getId()).isEqualTo(2);
        assertThat(token.getEmail()).isEqualTo("b@b.com");
        assertThat(token.getToken()).isEqualTo("tok");
        assertThat(token.getOtp()).isEqualTo("111111");
        assertThat(token.isUsed()).isTrue();
    }

    // ── Setters ──────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("changed@email.com");
        token.setToken("new-token");
        token.setOtp("999999");
        token.setUsed(true);

        assertThat(token.getEmail()).isEqualTo("changed@email.com");
        assertThat(token.getToken()).isEqualTo("new-token");
        assertThat(token.getOtp()).isEqualTo("999999");
        assertThat(token.isUsed()).isTrue();
    }

    // ── prePersist ────────────────────────────────────────────────────────

    @Test
    void prePersist_setsExpiresAt15MinutesFromNow() {
        PasswordResetToken token = new PasswordResetToken();
        assertThat(token.getExpiresAt()).isNull();

        LocalDateTime before = LocalDateTime.now();
        token.prePersist();
        LocalDateTime after = LocalDateTime.now();

        assertThat(token.getExpiresAt()).isNotNull();
        assertThat(token.getExpiresAt()).isAfterOrEqualTo(before.plusMinutes(14));
        assertThat(token.getExpiresAt()).isBeforeOrEqualTo(after.plusMinutes(16));
    }

    @Test
    void prePersist_expiresInFuture() {
        PasswordResetToken token = new PasswordResetToken();
        token.prePersist();
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    // ── isUsed ───────────────────────────────────────────────────────────

    @Test
    void used_defaultsFalse() {
        PasswordResetToken token = PasswordResetToken.builder()
                .email("x@x.com").token("t").otp("123456").used(false).build();
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void used_canBeSetTrue() {
        PasswordResetToken token = PasswordResetToken.builder().used(true).build();
        assertThat(token.isUsed()).isTrue();
    }

    // ── Lombok equals / hashCode ─────────────────────────────────────────

    @Test
    void equalTokens_areEqual() {
        LocalDateTime exp = LocalDateTime.now().plusMinutes(15);
        PasswordResetToken t1 = new PasswordResetToken(1, "a@a.com", "tok", "123456", exp, false);
        PasswordResetToken t2 = new PasswordResetToken(1, "a@a.com", "tok", "123456", exp, false);
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void differentTokens_areNotEqual() {
        PasswordResetToken t1 = PasswordResetToken.builder().token("tok1").build();
        PasswordResetToken t2 = PasswordResetToken.builder().token("tok2").build();
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    void toString_containsEmail() {
        PasswordResetToken token = PasswordResetToken.builder().email("test@x.com").build();
        assertThat(token.toString()).contains("test@x.com");
    }
}
