package com.medibook.auth.service.impl;

import com.medibook.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${medibook.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    private static final String OTP_PREFIX = "otp:";

    @Override
    public void generateAndSendOtp(String email) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Store in Redis with expiry
        redisTemplate.opsForValue().set(
            OTP_PREFIX + email,
            otp,
            otpExpiryMinutes,
            TimeUnit.MINUTES
        );

        // Send email
        sendOtpEmail(email, otp);
        log.info("OTP sent to email: {}", email);
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (storedOtp == null) {
            log.warn("OTP expired or not found for email: {}", email);
            return false;
        }
        boolean valid = storedOtp.equals(otp);
        if (valid) {
            clearOtp(email); // One-time use — delete after successful verification
        }
        return valid;
    }

    @Override
    public void clearOtp(String email) {
        redisTemplate.delete(OTP_PREFIX + email);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("MediBook — Email Verification OTP");
            message.setText(
                "Hello,\n\n" +
                "Your MediBook verification OTP is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for " + otpExpiryMinutes + " minutes.\n" +
                "Do NOT share this OTP with anyone.\n\n" +
                "If you did not register on MediBook, please ignore this email.\n\n" +
                "— MediBook Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please check your email address.");
        }
    }
}