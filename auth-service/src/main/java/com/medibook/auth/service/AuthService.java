package com.medibook.auth.service;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.entity.User;

public interface AuthService {
	
	// Step 1: Send OTP to email before actual registration
    void sendRegistrationOtp(String email);

    // Step 2: Verify OTP — returns true if valid
    boolean verifyRegistrationOtp(String email, String otp);

    // Step 3: Complete registration after OTP verified
    UserResponse register(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
    void logout(String token);
    boolean validateToken(String token);
    String refreshToken(String token);
    UserResponse getUserByEmail(String email);
    UserResponse getUserById(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void deactivateAccount(Long userId);
    void requestDeleteAccountOtp(String email);
    void deleteOwnAccountWithOtp(String email, String otp);

    void adminDeleteUser(Long userId);
}