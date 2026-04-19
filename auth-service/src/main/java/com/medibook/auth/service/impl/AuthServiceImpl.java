package com.medibook.auth.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.service.AuthService;
import com.medibook.auth.service.OtpService;
import com.medibook.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    private final OtpService otpService;
    // Redis for tracking verified emails
    private final StringRedisTemplate redisTemplate;

    private static final String EMAIL_VERIFIED_PREFIX = "email_verified:";

    
    // --------------- Sending OTP on email for Registration ----------------
    @Override
    public void sendRegistrationOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        otpService.generateAndSendOtp(email);
    }

    // ----------------- Verifying OTP ------------------------------
    @Override
    public boolean verifyRegistrationOtp(String email, String otp) {
        boolean valid = otpService.verifyOtp(email, otp);
        if (valid) {
            // Mark email as verified in Redis for 30 mins so register can proceed
            redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_PREFIX + email,
                "true",
                30,
                java.util.concurrent.TimeUnit.MINUTES
            );
        }
        return valid;
    }
    
    // ------------------ Register User with Role(PROVIDER, PATIENT, ADMIN) -------------------
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
    	// Check OTP was verified
        String verified = redisTemplate.opsForValue()
                .get(EMAIL_VERIFIED_PREFIX + request.getEmail());
        if (verified == null) {
            throw new IllegalStateException(
                "Email not verified. Please verify your email with OTP first.");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .provider(User.OAuthProvider.LOCAL)
                .isActive(true)
                .build();
        User saved = userRepository.save(user);
        // Clean up the verified flag
        redisTemplate.delete(EMAIL_VERIFIED_PREFIX + request.getEmail());
        log.info("New user registered: {} with role {}", saved.getEmail(), saved.getRole());
        return mapToUserResponse(saved);
    }

    // -------------------- Login --------------------------
    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.getIsActive()) {
            throw new IllegalStateException("Account is deactivated");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .userId(user.getUserId())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public String refreshToken(String oldToken) {
        if (!jwtUtil.validateToken(oldToken)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String email = jwtUtil.extractEmail(oldToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return mapToUserResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email)));
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return mapToUserResponse(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId)));
    }

    // -------------------- Update some fields of profile --------------------
    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfilePicUrl() != null) user.setProfilePicUrl(request.getProfilePicUrl());
        return mapToUserResponse(userRepository.save(user));
    }

    // --------------------- Change Old Password ----------------------
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // -------------------- Deactivate Account ------------------------
    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Account deactivated for userId: {}", userId);
    }
    
    // ---------------- Request OTP for Deleting Account ---------------------
    @Override
    public void requestDeleteAccountOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (!user.getIsActive()) {
            throw new IllegalStateException("Account is already deactivated");
        }

        otpService.generateAndSendOtp(email);
        log.info("Delete account OTP sent to {}", email);
    }

    // ----------------- Registered or Logged-In user can delete his/her own account -----------------
    @Override
    @Transactional
    public void deleteOwnAccountWithOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        boolean valid = otpService.verifyOtp(email, otp);
        if (!valid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        userRepository.delete(user);

        redisTemplate.delete("otp:" + email);
        redisTemplate.delete("email_verified:" + email);

        log.info("User deleted own account for email: {}", email);
    }

    // ----------------- Admin can also delete account of any user --------------
    @Override
    @Transactional
    public void adminDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Protection: prevent deleting admin account
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalStateException("Admin accounts cannot be deleted through this endpoint");
        }
        userRepository.delete(user);

        redisTemplate.delete("otp:" + user.getEmail());
        redisTemplate.delete("email_verified:" + user.getEmail());

        log.info("Admin deleted user account for userId: {}, email: {}", userId, user.getEmail());
    }

    // ------------------ Logout -----------------------------
    @Override
    public void logout(String token) {
        // In production: add token to a Redis blacklist with TTL = remaining expiry
        log.info("User logged out");
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .profilePicUrl(user.getProfilePicUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
