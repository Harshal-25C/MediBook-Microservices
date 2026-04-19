package com.medibook.auth.resource;

import com.medibook.auth.dto.request.*;
import com.medibook.auth.dto.response.*;
import com.medibook.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;


import com.medibook.auth.dto.request.DeleteAccountOtpRequest;
import com.medibook.auth.dto.request.OtpRequest;
import com.medibook.auth.dto.request.OtpVerifyRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and profile management")
public class AuthResource {

    private final AuthService authService;
    
    // ── OTP Endpoints ──────────────────────────────────────────────────────────
    @PostMapping("/send-otp")
    @Operation(summary = "Step 1: Send OTP to email for registration verification")
    public ResponseEntity<Map<String, String>> sendOtp(
            @Valid @RequestBody OtpRequest request) {
        authService.sendRegistrationOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "OTP sent to " + request.getEmail() + ". Valid for 10 minutes."
        ));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Step 2: Verify OTP — then call /register to complete signup")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        boolean valid = authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        if (valid) {
            return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "Email verified. Now call /api/v1/auth/register to complete registration."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                "message", "Invalid or expired OTP. Please request a new OTP."
            ));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (Patient or Provider)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate current session")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken, "tokenType", "Bearer"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password/{userId}")
    @Operation(summary = "Change user password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/deactivate/{userId}")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/delete-account/request-otp")
    @Operation(summary = "Send OTP to registered email before deleting own account")
    public ResponseEntity<Map<String, String>> requestDeleteAccountOtp(Principal principal) {
        authService.requestDeleteAccountOtp(principal.getName());
        return ResponseEntity.ok(Map.of(
                "message", "Delete account OTP sent to your registered email."
        ));
    }

    @DeleteMapping("/delete-account/confirm")
    @Operation(summary = "Verify OTP and permanently delete own account")
    public ResponseEntity<Map<String, String>> confirmDeleteAccount(
            Principal principal,
            @Valid @RequestBody DeleteAccountOtpRequest request) {

        authService.deleteOwnAccountWithOtp(principal.getName(), request.getOtp());

        return ResponseEntity.ok(Map.of(
                "message", "Your account has been deleted permanently."
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Admin can permanently delete any user by userId")
    public ResponseEntity<Map<String, String>> adminDeleteUser(@PathVariable Long userId) {
        authService.adminDeleteUser(userId);
        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully by admin."
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate a JWT token")
    public ResponseEntity<Map<String, Boolean>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(Map.of("valid", authService.validateToken(token)));
    }
}