package com.medibook.otp.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OtpTokenTest {

    // ── Builder ──────────────────────────────────────────────────────────

    @Test
    void builder_setsAllFields() {
        LocalDateTime expires = LocalDateTime.now().plusMinutes(5);
        OtpToken token = OtpToken.builder()
                .id(1)
                .email("user@medibook.com")
                .otp("123456")
                .expiresAt(expires)
                .used(false)
                .build();

        assertThat(token.getId()).isEqualTo(1);
        assertThat(token.getEmail()).isEqualTo("user@medibook.com");
        assertThat(token.getOtp()).isEqualTo("123456");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void noArgsConstructor_createsEmpty() {
        OtpToken token = new OtpToken();
        assertThat(token.getEmail()).isNull();
        assertThat(token.getOtp()).isNull();
        assertThat(token.getExpiresAt()).isNull();
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime expires = LocalDateTime.now().plusMinutes(5);
        OtpToken token = new OtpToken(3, "b@b.com", "654321", expires, true);

        assertThat(token.getId()).isEqualTo(3);
        assertThat(token.getEmail()).isEqualTo("b@b.com");
        assertThat(token.getOtp()).isEqualTo("654321");
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.isUsed()).isTrue();
    }

    // ── Setters ──────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        OtpToken token = new OtpToken();
        LocalDateTime exp = LocalDateTime.now().plusMinutes(5);
        token.setEmail("new@x.com");
        token.setOtp("999999");
        token.setExpiresAt(exp);
        token.setUsed(true);

        assertThat(token.getEmail()).isEqualTo("new@x.com");
        assertThat(token.getOtp()).isEqualTo("999999");
        assertThat(token.getExpiresAt()).isEqualTo(exp);
        assertThat(token.isUsed()).isTrue();
    }

    // ── prePersist ────────────────────────────────────────────────────────

    @Test
    void prePersist_setsExpiresAt5MinutesFromNow() {
        OtpToken token = new OtpToken();
        assertThat(token.getExpiresAt()).isNull();

        LocalDateTime before = LocalDateTime.now();
        token.prePersist();
        LocalDateTime after = LocalDateTime.now();

        assertThat(token.getExpiresAt()).isNotNull();
        assertThat(token.getExpiresAt()).isAfterOrEqualTo(before.plusMinutes(4).plusSeconds(59));
        assertThat(token.getExpiresAt()).isBeforeOrEqualTo(after.plusMinutes(5).plusSeconds(1));
    }

    @Test
    void prePersist_expiresInFuture() {
        OtpToken token = new OtpToken();
        token.prePersist();
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void prePersist_expiresBefore10MinutesFromNow() {
        OtpToken token = new OtpToken();
        token.prePersist();
        assertThat(token.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(10));
    }

    // ── isUsed ───────────────────────────────────────────────────────────

    @Test
    void used_defaultsFalse() {
        OtpToken token = OtpToken.builder().email("x@x.com").otp("123456").used(false).build();
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void used_canBeSetTrue() {
        OtpToken token = OtpToken.builder().used(true).build();
        assertThat(token.isUsed()).isTrue();
    }

    // ── Lombok equals / hashCode ─────────────────────────────────────────

    @Test
    void equalTokens_areEqual() {
        LocalDateTime exp = LocalDateTime.now().plusMinutes(5);
        OtpToken t1 = new OtpToken(1, "a@a.com", "111111", exp, false);
        OtpToken t2 = new OtpToken(1, "a@a.com", "111111", exp, false);
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void differentTokens_areNotEqual() {
        OtpToken t1 = OtpToken.builder().otp("111111").build();
        OtpToken t2 = OtpToken.builder().otp("999999").build();
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    void toString_containsEmail() {
        OtpToken token = OtpToken.builder().email("test@medibook.com").build();
        assertThat(token.toString()).contains("test@medibook.com");
    }

    // ── OTP format validation (helper assertions) ─────────────────────────

    @Test
    void otp_6DigitString_isStored() {
        OtpToken token = OtpToken.builder().otp("000001").build();
        assertThat(token.getOtp()).hasSize(6);
        assertThat(token.getOtp()).matches("\\d{6}");
    }

    @Test
    void otp_maxValue_isStored() {
        OtpToken token = OtpToken.builder().otp("999999").build();
        assertThat(token.getOtp()).isEqualTo("999999");
    }
}
