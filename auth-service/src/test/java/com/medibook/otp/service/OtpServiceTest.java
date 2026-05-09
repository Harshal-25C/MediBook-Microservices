package com.medibook.otp.service;

import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.entity.OtpToken;
import com.medibook.otp.repository.OtpRepository;
import com.medibook.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpRepository otpRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private UserRepository userRepository;

    private OtpService otpService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        injectField(otpService, "otpRepository", otpRepository);
        injectField(otpService, "mailSender", mailSender);
        injectField(otpService, "userRepository", userRepository);

        activeUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@medibook.com")
                .role("Patient")
                .isActive(true)
                .build();
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── generateAndSendOtp ────────────────────────────────────────────────

    @Test
    void generateAndSendOtp_userExists_deletesOldAndSavesNew() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(otpRepository).deleteAllByEmail("john@medibook.com");
        when(otpRepository.save(any(OtpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp("john@medibook.com");

        verify(otpRepository).deleteAllByEmail("john@medibook.com");
        verify(otpRepository).save(any(OtpToken.class));
    }

    @Test
    void generateAndSendOtp_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.generateAndSendOtp("nobody@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(otpRepository, never()).save(any());
    }

    @Test
    void generateAndSendOtp_savesOtpWithCorrectEmail() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(otpRepository).deleteAllByEmail(any());

        ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
        when(otpRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp("john@medibook.com");

        assertThat(captor.getValue().getEmail()).isEqualTo("john@medibook.com");
        assertThat(captor.getValue().isUsed()).isFalse();
    }

    @Test
    void generateAndSendOtp_otp_is6Digits() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(otpRepository).deleteAllByEmail(any());

        ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
        when(otpRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp("john@medibook.com");

        String otp = captor.getValue().getOtp();
        assertThat(otp).hasSize(6).matches("\\d{6}");
    }

    @Test
    void generateAndSendOtp_sendsEmail() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(otpRepository).deleteAllByEmail(any());
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp("john@medibook.com");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void generateAndSendOtp_emailFailure_doesNotThrow() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(otpRepository).deleteAllByEmail(any());
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> otpService.generateAndSendOtp("john@medibook.com"))
                .doesNotThrowAnyException();
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────

    @Test
    void verifyOtp_validOtp_returnsTrue() {
        OtpToken token = OtpToken.builder()
                .email("john@medibook.com")
                .otp("123456")
                .used(false)
                .build();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(3));

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("john@medibook.com"))
                .thenReturn(Optional.of(token));

        boolean result = otpService.verifyOtp("john@medibook.com", "123456");

        assertThat(result).isTrue();
        verify(otpRepository).delete(token);
    }

    @Test
    void verifyOtp_noOtpFound_throwsBadRequestException() {
        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("unknown@x.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp("unknown@x.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No OTP found");
    }

    @Test
    void verifyOtp_expiredOtp_throwsBadRequestException() {
        OtpToken token = OtpToken.builder()
                .email("john@medibook.com")
                .otp("123456")
                .used(false)
                .build();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("john@medibook.com"))
                .thenReturn(Optional.of(token));
        doNothing().when(otpRepository).delete(token);

        assertThatThrownBy(() -> otpService.verifyOtp("john@medibook.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyOtp_wrongOtp_throwsBadRequestException() {
        OtpToken token = OtpToken.builder()
                .email("john@medibook.com")
                .otp("111111")
                .used(false)
                .build();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(3));

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("john@medibook.com"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp("john@medibook.com", "999999"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void verifyOtp_expiredOtp_deletesFromRepository() {
        OtpToken token = OtpToken.builder()
                .email("john@medibook.com")
                .otp("111111")
                .used(false)
                .build();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(30));

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("john@medibook.com"))
                .thenReturn(Optional.of(token));
        doNothing().when(otpRepository).delete(token);

        assertThatThrownBy(() -> otpService.verifyOtp("john@medibook.com", "111111"))
                .isInstanceOf(BadRequestException.class);

        verify(otpRepository).delete(token);
    }

    @Test
    void verifyOtp_validOtp_deletesTokenAfterVerification() {
        OtpToken token = OtpToken.builder()
                .email("john@medibook.com")
                .otp("654321")
                .used(false)
                .build();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc("john@medibook.com"))
                .thenReturn(Optional.of(token));

        otpService.verifyOtp("john@medibook.com", "654321");

        verify(otpRepository, times(1)).delete(token);
    }
}
