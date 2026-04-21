package com.medibook.auth.service;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.entity.User;
import com.medibook.auth.dto.response.AuthResponse;

public interface AuthService {
	
    User register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserByEmail(String email);

    User getUserById(int userId);

    User updateProfile(int userId, User updatedUser);

    void changePassword(int userId, String newPassword);

    void deactivateAccount(int userId);
    
     User registerAdmin(RegisterAdminRequest request, String adminSecretCode);

    User findOrCreateGoogleUser(String email, String fullName, String picture, String provider, String role);
    
    void sendOtp(String email);
    boolean verifyOtp(String email, String otp);

 // Forgot password — send reset link + OTP to email
    void forgotPassword(String email);

    // Verify reset OTP — check token + OTP are valid
    void verifyResetOtp(String token, String otp);

    // Reset password — save new password after OTP verified
    void resetPassword(String token, String newPassword);
}