package com.medibook.auth.resource;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
// @CrossOrigin REMOVED — CORS is handled entirely by the API Gateway (application.yml globalcors).
// Adding @CrossOrigin here causes duplicate Access-Control-Allow-Origin headers → browser rejects.
public class AuthResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.admin.secret.code}")
    private String adminSecretCode;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful",
                        "userId", user.getUserId(),
                        "role", user.getRole()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        AuthResponse auth = authService.login(request);

        User user = authService.getUserByEmail(request.getEmail());
        if (!user.getRole().equals("Admin")
                && (user.getPhone() == null || user.getPhone().trim().isEmpty())
                && user.getProvider() == null) {
            return ResponseEntity.ok(Map.of(
                        "message", "Phone number required",
                        "requiresPhone", true,
                        "email", request.getEmail()
            ));
        }

        authService.sendOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "otpSent", true,
                "email", request.getEmail(),
                "message", "OTP sent to your email"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        authService.verifyOtp(email, otp);

        User user = authService.getUserByEmail(email);
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "userId",   user.getUserId(),
                "role",     user.getRole(),
                "fullName", user.getFullName(),
                "message",  "Login successful"
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authService.sendOtp(email);
        return ResponseEntity.ok(Map.of(
                "otpSent", true,
                "message", "New OTP sent to your email"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable int userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(
            @PathVariable int userId,
            @RequestBody User updatedUser) {
        return ResponseEntity.ok(authService.updateProfile(userId, updatedUser));
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable int userId,
            @RequestBody Map<String, String> body) {
        authService.changePassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivate(@PathVariable int userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        try {
            User admin = authService.registerAdmin(request, adminSecretCode);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Admin account created successfully",
                            "userId", admin.getUserId(),
                            "email", admin.getEmail()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/google/complete")
    public ResponseEntity<?> completeGoogleLogin(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String fullName = body.get("fullName");
        String picture  = body.get("picture");
        String provider = body.get("provider");
        String role     = body.get("role");

        User user = authService.findOrCreateGoogleUser(email, fullName, picture, provider, role);

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "userId",   user.getUserId(),
                "role",     user.getRole(),
                "fullName", user.getFullName()
        ));
    }

    @PostMapping("/add-phone")
    public ResponseEntity<?> addPhone(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String phone  = body.get("phone");

        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Phone number is required."));
        }

        User user = authService.getUserByEmail(email);
        user.setPhone(phone);
        authService.updateProfile(user.getUserId(), user);
        authService.sendOtp(email);

        return ResponseEntity.ok(Map.of(
                "otpSent", true,
                "message", "Phone saved and OTP sent to your email"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }

        try {
            authService.forgotPassword(email.trim());
            return ResponseEntity.ok(Map.of(
                    "sent", true,
                    "message", "Reset link sent to your email"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "sent", true,
                    "message", "If this email is registered, a reset link has been sent"
            ));
        }
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String otp   = body.get("otp");

        authService.verifyResetOtp(token, otp);

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "OTP verified. You can now reset your password."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");

        authService.resetPassword(token, newPassword);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password reset successful. Please login with your new password."
        ));
    }

    // ── Admin User Management Endpoints ──────────────────────────────────

    /**
     * GET /auth/users — returns all users (Admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * GET /auth/users/role/{role} — returns users filtered by role
     * role values: Patient, Provider, Admin
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    /**
     * PUT /auth/reactivate/{userId} — re-enable a deactivated user account
     */
    @PutMapping("/reactivate/{userId}")
    public ResponseEntity<?> reactivate(@PathVariable int userId) {
        authService.reactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account reactivated successfully"));
    }
}
