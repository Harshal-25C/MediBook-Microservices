package com.medibook.otp.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.entity.OtpToken;
import com.medibook.otp.repository.OtpRepository;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    // ── Generate and send OTP ──────────────────────────────────────────
    @Transactional
    public void generateAndSendOtp(String email) {

        // Check user exists
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Delete any old OTPs for this email first
        otpRepository.deleteAllByEmail(email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save to DB (expiresAt set automatically in @PrePersist)
        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .used(false)
                .build();
        otpRepository.save(otpToken);

        // ── Send OTP via EMAIL (Gmail SMTP) ────────────────────────────
        sendOtpEmail(email, otp);

        // ── Print OTP in CONSOLE (for SMS simulation during development) ──
        System.out.println("========================================");
        System.out.println("  MediBook OTP for: " + email);
        System.out.println("  OTP CODE: " + otp);
        System.out.println("  Expires in 5 minutes");
        System.out.println("========================================");
    }

    // ── Verify OTP ────────────────────────────────────────────────────
    @Transactional
    public boolean verifyOtp(String email, String otp) {

        OtpToken otpToken = otpRepository
                .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email)
                .orElseThrow(() -> new BadRequestException(
                        "No OTP found for this email. Please request a new one."));

        // Check expired
        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpToken);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Check wrong OTP
        if (!otpToken.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        // Mark as used and delete
        otpRepository.delete(otpToken);
        return true;
    }

    // ── Send OTP Email ────────────────────────────────────────────────
    private void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("MediBook — Your OTP Code");
            message.setText(
                "Hello,\n\n" +
                "Your MediBook verification code is:\n\n" +
                "  " + otp + "\n\n" +
                "This code is valid for 5 minutes.\n" +
                "Do not share this code with anyone.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— MediBook Team"
            );
            mailSender.send(message);
            System.out.println("[OtpService] Email OTP sent to: " + toEmail);
        } catch (Exception e) {
            // Log error but don't break the flow — OTP still printed in console
            System.err.println("[OtpService] Email sending failed: " + e.getMessage());
        }
    }
}