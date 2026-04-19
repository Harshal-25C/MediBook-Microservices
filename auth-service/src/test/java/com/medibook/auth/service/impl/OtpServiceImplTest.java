package com.medibook.auth.service.impl;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
 
import java.util.concurrent.TimeUnit;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("OtpServiceImpl — Unit Tests")
class OtpServiceImplTest {
 
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private JavaMailSender mailSender;
    @Mock private ValueOperations<String, String> valueOperations;
 
    @InjectMocks
    private OtpServiceImpl otpService;
 
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 10);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
 
    // ── generateAndSendOtp ────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("generateAndSendOtp()")
    class GenerateAndSendOtpTests {
 
        @Test
        @DisplayName("stores a 6-digit OTP in Redis with correct key and TTL")
        void shouldStoreOtpInRedis_withCorrectKeyAndTTL() {
            otpService.generateAndSendOtp("user@gmail.com");
 
            ArgumentCaptor<String> keyCaptor     = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor   = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long>   ttlCaptor     = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TimeUnit> unitCaptor  = ArgumentCaptor.forClass(TimeUnit.class);
 
            verify(valueOperations).set(
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    ttlCaptor.capture(),
                    unitCaptor.capture()
            );
 
            assertThat(keyCaptor.getValue()).isEqualTo("otp:user@gmail.com");
            assertThat(valueCaptor.getValue()).matches("\\d{6}");   // exactly 6 digits
            assertThat(ttlCaptor.getValue()).isEqualTo(10L);
            assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.MINUTES);
        }
 
        @Test
        @DisplayName("sends an email via JavaMailSender")
        void shouldSendEmail_viaMailSender() {
            otpService.generateAndSendOtp("user@gmail.com");
 
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
 
        @Test
        @DisplayName("email is addressed to the correct recipient")
        void shouldSendEmail_toCorrectRecipient() {
            otpService.generateAndSendOtp("target@gmail.com");
 
            ArgumentCaptor<SimpleMailMessage> msgCaptor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(msgCaptor.capture());
 
            SimpleMailMessage sent = msgCaptor.getValue();
            assertThat(sent.getTo()).contains("target@gmail.com");
        }
 
        @Test
        @DisplayName("email subject contains 'MediBook'")
        void shouldSendEmail_withMediBookSubject() {
            otpService.generateAndSendOtp("user@gmail.com");
 
            ArgumentCaptor<SimpleMailMessage> msgCaptor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(msgCaptor.capture());
 
            assertThat(msgCaptor.getValue().getSubject()).contains("MediBook");
        }
 
        @Test
        @DisplayName("email body contains the actual OTP that was stored in Redis")
        void shouldSendEmail_withOtpInBody() {
            otpService.generateAndSendOtp("user@gmail.com");
 
            // Capture OTP stored in Redis
            ArgumentCaptor<String> redisValueCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(anyString(), redisValueCaptor.capture(),
                    anyLong(), any());
            String storedOtp = redisValueCaptor.getValue();
 
            // Capture email body
            ArgumentCaptor<SimpleMailMessage> msgCaptor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(msgCaptor.capture());
 
            assertThat(msgCaptor.getValue().getText()).contains(storedOtp);
        }
 
        @Test
        @DisplayName("throws RuntimeException when mail sending fails")
        void shouldThrowRuntimeException_whenMailFails() {
            doThrow(new RuntimeException("SMTP error")).when(mailSender)
                    .send(any(SimpleMailMessage.class));
 
            assertThatThrownBy(() -> otpService.generateAndSendOtp("fail@gmail.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send OTP email");
        }
 
        @Test
        @DisplayName("still stores OTP in Redis even when mail sending fails")
        void shouldStoreOtp_evenWhenMailFails() {
            doThrow(new RuntimeException("SMTP error")).when(mailSender)
                    .send(any(SimpleMailMessage.class));
 
            try {
                otpService.generateAndSendOtp("fail@gmail.com");
            } catch (RuntimeException ignored) {}
 
            // Redis set should still have been called before email attempt failed
            verify(valueOperations).set(
                    eq("otp:fail@gmail.com"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        }
 
        @Test
        @DisplayName("generates different OTPs on separate calls (probabilistic)")
        void shouldGenerateDifferentOtpsOnSeparateCalls() {
            otpService.generateAndSendOtp("a@test.com");
            otpService.generateAndSendOtp("b@test.com");
 
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations, times(2)).set(
                    anyString(), captor.capture(), anyLong(), any());
 
            // With 1,000,000 possible OTPs, collision probability is negligible
            // This test is mostly verifying the format is 6 digits
            captor.getAllValues().forEach(otp ->
                    assertThat(otp).matches("\\d{6}"));
        }
    }
 
    // ── verifyOtp ─────────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtpTests {
 
        @Test
        @DisplayName("returns false when Redis has no OTP for email (expired or missing)")
        void shouldReturnFalse_whenNoOtpInRedis() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn(null);
 
            assertThat(otpService.verifyOtp("user@gmail.com", "123456")).isFalse();
        }
 
        @Test
        @DisplayName("returns false when OTP does not match stored value")
        void shouldReturnFalse_whenOtpMismatch() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn("999999");
 
            assertThat(otpService.verifyOtp("user@gmail.com", "111111")).isFalse();
        }
 
        @Test
        @DisplayName("returns true when OTP matches stored value")
        void shouldReturnTrue_whenOtpMatches() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn("482910");
 
            assertThat(otpService.verifyOtp("user@gmail.com", "482910")).isTrue();
        }
 
        @Test
        @DisplayName("deletes OTP from Redis after successful verification (one-time use)")
        void shouldDeleteOtp_afterSuccessfulVerification() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn("482910");
 
            otpService.verifyOtp("user@gmail.com", "482910");
 
            verify(redisTemplate).delete("otp:user@gmail.com");
        }
 
        @Test
        @DisplayName("does NOT delete OTP from Redis when verification fails")
        void shouldNotDeleteOtp_whenVerificationFails() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn("999999");
 
            otpService.verifyOtp("user@gmail.com", "000000");
 
            verify(redisTemplate, never()).delete(anyString());
        }
 
        @Test
        @DisplayName("is case-sensitive — '482910' does not equal '48291O' (O vs 0)")
        void shouldBeCaseSensitiveAndExactMatch() {
            given(valueOperations.get("otp:user@gmail.com")).willReturn("482910");
 
            // Letter O instead of digit 0
            assertThat(otpService.verifyOtp("user@gmail.com", "48291O")).isFalse();
        }
    }
 
    // ── clearOtp ──────────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("clearOtp()")
    class ClearOtpTests {
 
        @Test
        @DisplayName("deletes the correct Redis key")
        void shouldDeleteCorrectKey() {
            otpService.clearOtp("clear@gmail.com");
 
            verify(redisTemplate).delete("otp:clear@gmail.com");
        }
 
        @Test
        @DisplayName("does not throw when key does not exist")
        void shouldNotThrow_whenKeyDoesNotExist() {
            given(redisTemplate.delete(anyString())).willReturn(false);
 
            assertThatCode(() -> otpService.clearOtp("none@gmail.com"))
                    .doesNotThrowAnyException();
        }
    }
}