package com.medibook.otp.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.auth.entity.User;
import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.entity.OtpToken;
import com.medibook.otp.repository.OtpRepository;

import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;
    
    private static final Logger logger =
            LoggerFactory.getLogger(OtpService.class);
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Generate and send OTP ──────────────────────────────────────────
    @Transactional
    public void generateAndSendOtp(String email) {

    	// ✅ FIXED — actually use the returned user
    	User user = userRepository.findByEmail(email)
    	    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    	// Now 'user' is available if you need it later in the method
    	// Even if you don't use user, assigning it satisfies SonarQube

        // Delete any old OTPs for this email first
        otpRepository.deleteAllByEmail(email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(999999));

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
        logger.info("Generating OTP for user: {}", user.getFullName());
        logger.info("===============================================");
        logger.info("MediBook OTP generated for email: {}", email);
        logger.info("OTP CODE: {}", otp);
        logger.info("OTP expires in 5 minutes");
        logger.info("===============================================");
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