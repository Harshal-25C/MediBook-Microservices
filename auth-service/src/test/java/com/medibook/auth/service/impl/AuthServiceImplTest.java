package com.medibook.auth.service.impl;
 
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.UpdateProfileRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.service.OtpService;
import com.medibook.auth.util.JwtUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
 
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — Unit Tests")
class AuthServiceImplTest {
 
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private OtpService otpService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
 
    @InjectMocks
    private AuthServiceImpl authService;
 
    // ── Helper: build a standard test User ───────────────────────────────────
 
    private User buildUser(Long id, String email, User.Role role) {
        return User.builder()
                .userId(id)
                .fullName("Test User")
                .email(email)
                .passwordHash("$2a$12$hashedpassword")
                .phone("9876543210")
                .role(role)
                .provider(User.OAuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
 
    // ── sendRegistrationOtp ───────────────────────────────────────────────────
 
    @Nested
    @DisplayName("sendRegistrationOtp()")
    class SendRegistrationOtpTests {
 
        @Test
        @DisplayName("throws IllegalArgumentException when email already registered")
        void shouldThrow_whenEmailAlreadyExists() {
            given(userRepository.existsByEmail("existing@gmail.com")).willReturn(true);
 
            assertThatThrownBy(() -> authService.sendRegistrationOtp("existing@gmail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is already registered.");
 
            verify(otpService, never()).generateAndSendOtp(any());
        }
 
        @Test
        @DisplayName("delegates to OtpService when email is new")
        void shouldCallOtpService_whenEmailIsNew() {
            given(userRepository.existsByEmail("new@gmail.com")).willReturn(false);
 
            authService.sendRegistrationOtp("new@gmail.com");
 
            verify(otpService, times(1)).generateAndSendOtp("new@gmail.com");
        }
    }
 
    // ── verifyRegistrationOtp ─────────────────────────────────────────────────
 
    @Nested
    @DisplayName("verifyRegistrationOtp()")
    class VerifyRegistrationOtpTests {

        @Test
        @DisplayName("returns true and sets Redis verified flag when OTP is valid")
        void shouldReturnTrue_andSetRedisFlag_whenOtpValid() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(otpService.verifyOtp("user@gmail.com", "123456")).willReturn(true);

            boolean result = authService.verifyRegistrationOtp("user@gmail.com", "123456");

            assertThat(result).isTrue();
            verify(valueOperations).set(
                    eq("email_verified:user@gmail.com"),
                    eq("true"),
                    eq(30L),
                    eq(TimeUnit.MINUTES)
            );
        }

        @Test
        @DisplayName("returns false and does NOT set Redis flag when OTP is invalid")
        void shouldReturnFalse_andNotSetRedisFlag_whenOtpInvalid() {
            given(otpService.verifyOtp("user@gmail.com", "000000")).willReturn(false);

            boolean result = authService.verifyRegistrationOtp("user@gmail.com", "000000");

            assertThat(result).isFalse();
            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
        }
    }
 
    // ── register ─────────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("register()")
    class RegisterTests {
 
        @BeforeEach
        void setupRedis() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }
 
        private RegisterRequest buildRequest(String email, User.Role role) {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Rahul Sharma");
            req.setEmail(email);
            req.setPassword("Rahul@123");
            req.setPhone("9876543210");
            req.setRole(role);
            return req;
        }
 
        @Test
        @DisplayName("throws IllegalStateException when email not verified in Redis")
        void shouldThrow_whenEmailNotVerified() {
            given(valueOperations.get("email_verified:new@gmail.com")).willReturn(null);
 
            RegisterRequest req = buildRequest("new@gmail.com", User.Role.PATIENT);
 
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email not verified");
 
            verify(userRepository, never()).save(any());
        }
 
        @Test
        @DisplayName("throws IllegalArgumentException when email already in DB")
        void shouldThrow_whenEmailAlreadyInDatabase() {
            given(valueOperations.get("email_verified:dup@gmail.com")).willReturn("true");
            given(userRepository.existsByEmail("dup@gmail.com")).willReturn(true);
 
            RegisterRequest req = buildRequest("dup@gmail.com", User.Role.PATIENT);
 
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already registered");
 
            verify(userRepository, never()).save(any());
        }
 
        @Test
        @DisplayName("saves user with BCrypt-encoded password")
        void shouldSaveUser_withEncodedPassword() {
            String email = "rahul@gmail.com";
            given(valueOperations.get("email_verified:" + email)).willReturn("true");
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode("Rahul@123")).willReturn("$2a$12$encoded");
 
            User saved = buildUser(1L, email, User.Role.PATIENT);
            given(userRepository.save(any(User.class))).willReturn(saved);
 
            RegisterRequest req = buildRequest(email, User.Role.PATIENT);
            authService.register(req);
 
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded");
        }
 
        @Test
        @DisplayName("returns correct UserResponse after successful registration")
        void shouldReturnCorrectUserResponse() {
            String email = "priya@gmail.com";
            given(valueOperations.get("email_verified:" + email)).willReturn("true");
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$12$encoded");
 
            User saved = buildUser(5L, email, User.Role.PROVIDER);
            given(userRepository.save(any(User.class))).willReturn(saved);
 
            RegisterRequest req = buildRequest(email, User.Role.PROVIDER);
            UserResponse response = authService.register(req);
 
            assertThat(response.getUserId()).isEqualTo(5L);
            assertThat(response.getEmail()).isEqualTo(email);
            assertThat(response.getRole()).isEqualTo("PROVIDER");
            assertThat(response.getIsActive()).isTrue();
        }
 
        @Test
        @DisplayName("deletes Redis verified flag after successful registration")
        void shouldDeleteRedisFlag_afterRegistration() {
            String email = "cleanup@gmail.com";
            given(valueOperations.get("email_verified:" + email)).willReturn("true");
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(userRepository.save(any())).willReturn(buildUser(1L, email, User.Role.PATIENT));
 
            authService.register(buildRequest(email, User.Role.PATIENT));
 
            verify(redisTemplate).delete("email_verified:" + email);
        }
    }
 
    // ── login ─────────────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("login()")
    class LoginTests {
 
        private LoginRequest buildLogin(String email, String password) {
            LoginRequest req = new LoginRequest();
            req.setEmail(email);
            req.setPassword(password);
            return req;
        }
 
        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findByEmail("ghost@gmail.com")).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.login(buildLogin("ghost@gmail.com", "pass")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }
 
        @Test
        @DisplayName("throws IllegalStateException when account is deactivated")
        void shouldThrow_whenAccountDeactivated() {
            User user = buildUser(1L, "deactivated@gmail.com", User.Role.PATIENT);
            user.setIsActive(false);
            given(userRepository.findByEmail("deactivated@gmail.com")).willReturn(Optional.of(user));
 
            assertThatThrownBy(
                    () -> authService.login(buildLogin("deactivated@gmail.com", "pass")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Account is deactivated");
        }
 
        @Test
        @DisplayName("throws IllegalArgumentException when password is wrong")
        void shouldThrow_whenPasswordWrong() {
            User user = buildUser(1L, "user@gmail.com", User.Role.PATIENT);
            given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongpass", user.getPasswordHash())).willReturn(false);
 
            assertThatThrownBy(() -> authService.login(buildLogin("user@gmail.com", "wrongpass")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid credentials");
        }
 
        @Test
        @DisplayName("returns AuthResponse with Bearer token on valid credentials")
        void shouldReturnAuthResponse_onValidCredentials() {
            User user = buildUser(1L, "admin@gmail.com", User.Role.ADMIN);
            given(userRepository.findByEmail("admin@gmail.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Admin@123", user.getPasswordHash())).willReturn(true);
            given(jwtUtil.generateToken("admin@gmail.com", "ADMIN", 1L))
                    .willReturn("mocked.jwt.token");
 
            AuthResponse response = authService.login(buildLogin("admin@gmail.com", "Admin@123"));
 
            assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getEmail()).isEqualTo("admin@gmail.com");
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getUserId()).isEqualTo(1L);
        }
    }
 
    // ── validateToken ─────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTests {
 
        @Test
        @DisplayName("delegates to JwtUtil and returns true for valid token")
        void shouldReturnTrue_forValidToken() {
            given(jwtUtil.validateToken("valid.token")).willReturn(true);
            assertThat(authService.validateToken("valid.token")).isTrue();
        }
 
        @Test
        @DisplayName("delegates to JwtUtil and returns false for invalid token")
        void shouldReturnFalse_forInvalidToken() {
            given(jwtUtil.validateToken("invalid.token")).willReturn(false);
            assertThat(authService.validateToken("invalid.token")).isFalse();
        }
    }
 
    // ── refreshToken ──────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {
 
        @Test
        @DisplayName("throws IllegalArgumentException when old token is invalid")
        void shouldThrow_whenOldTokenInvalid() {
            given(jwtUtil.validateToken("bad.token")).willReturn(false);
 
            assertThatThrownBy(() -> authService.refreshToken("bad.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid or expired token");
        }
 
        @Test
        @DisplayName("returns new token when old token is valid")
        void shouldReturnNewToken_whenOldTokenValid() {
            User user = buildUser(1L, "user@gmail.com", User.Role.PATIENT);
            given(jwtUtil.validateToken("old.token")).willReturn(true);
            given(jwtUtil.extractEmail("old.token")).willReturn("user@gmail.com");
            given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
            given(jwtUtil.generateToken("user@gmail.com", "PATIENT", 1L))
                    .willReturn("new.jwt.token");
 
            String result = authService.refreshToken("old.token");
 
            assertThat(result).isEqualTo("new.jwt.token");
        }
    }
 
    // ── getUserById ───────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when userId does not exist")
        void shouldThrow_whenNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
 
        @Test
        @DisplayName("returns correct UserResponse when user exists")
        void shouldReturnUserResponse_whenFound() {
            User user = buildUser(5L, "found@gmail.com", User.Role.PROVIDER);
            given(userRepository.findById(5L)).willReturn(Optional.of(user));
 
            UserResponse response = authService.getUserById(5L);
 
            assertThat(response.getUserId()).isEqualTo(5L);
            assertThat(response.getEmail()).isEqualTo("found@gmail.com");
            assertThat(response.getRole()).isEqualTo("PROVIDER");
        }
    }
 
    // ── getUserByEmail ────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmailTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when email not found")
        void shouldThrow_whenEmailNotFound() {
            given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.getUserByEmail("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("returns UserResponse when email exists")
        void shouldReturn_whenEmailExists() {
            User user = buildUser(2L, "exist@test.com", User.Role.PATIENT);
            given(userRepository.findByEmail("exist@test.com")).willReturn(Optional.of(user));
 
            UserResponse response = authService.getUserByEmail("exist@test.com");
            assertThat(response.getEmail()).isEqualTo("exist@test.com");
        }
    }
 
    // ── updateProfile ─────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when userId not found")
        void shouldThrow_whenNotFound() {
            given(userRepository.findById(77L)).willReturn(Optional.empty());
 
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFullName("New Name");
 
            assertThatThrownBy(() -> authService.updateProfile(77L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("updates only fullName when only fullName is provided")
        void shouldUpdateOnlyFullName() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
 
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFullName("Updated Name");
 
            authService.updateProfile(1L, req);
 
            assertThat(user.getFullName()).isEqualTo("Updated Name");
            assertThat(user.getPhone()).isEqualTo("9876543210"); // unchanged
        }
 
        @Test
        @DisplayName("updates phone and profilePicUrl when provided")
        void shouldUpdatePhoneAndPicUrl() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
 
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setPhone("9111111111");
            req.setProfilePicUrl("https://pic.com/new.jpg");
 
            authService.updateProfile(1L, req);
 
            assertThat(user.getPhone()).isEqualTo("9111111111");
            assertThat(user.getProfilePicUrl()).isEqualTo("https://pic.com/new.jpg");
        }
 
        @Test
        @DisplayName("does not update fields that are null in request")
        void shouldNotUpdateNullFields() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            user.setProfilePicUrl("https://original.com/pic.jpg");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
 
            UpdateProfileRequest req = new UpdateProfileRequest(); // all nulls
 
            authService.updateProfile(1L, req);
 
            assertThat(user.getFullName()).isEqualTo("Test User");      // unchanged
            assertThat(user.getProfilePicUrl())
                    .isEqualTo("https://original.com/pic.jpg");          // unchanged
        }
    }
 
    // ── changePassword ────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when userId not found")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(55L)).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.changePassword(55L, "old", "new"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("throws IllegalArgumentException when old password is wrong")
        void shouldThrow_whenOldPasswordWrong() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongOld", user.getPasswordHash())).willReturn(false);
 
            assertThatThrownBy(() -> authService.changePassword(1L, "wrongOld", "newPass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Current password is incorrect");
        }
 
        @Test
        @DisplayName("encodes and saves new password when old password is correct")
        void shouldEncodeAndSaveNewPassword() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("OldPass@1", user.getPasswordHash())).willReturn(true);
            given(passwordEncoder.encode("NewPass@2")).willReturn("$2a$12$newEncoded");
 
            authService.changePassword(1L, "OldPass@1", "NewPass@2");
 
            assertThat(user.getPasswordHash()).isEqualTo("$2a$12$newEncoded");
            verify(userRepository).save(user);
        }
    }
 
    // ── deactivateAccount ─────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("deactivateAccount()")
    class DeactivateAccountTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when userId not found")
        void shouldThrow_whenNotFound() {
            given(userRepository.findById(33L)).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.deactivateAccount(33L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("sets isActive to false and saves user")
        void shouldSetIsActiveFalse() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            assertThat(user.getIsActive()).isTrue();
 
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
 
            authService.deactivateAccount(1L);
 
            assertThat(user.getIsActive()).isFalse();
            verify(userRepository).save(user);
        }
    }
 
    // ── requestDeleteAccountOtp ───────────────────────────────────────────────
 
    @Nested
    @DisplayName("requestDeleteAccountOtp()")
    class RequestDeleteAccountOtpTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when email not found")
        void shouldThrow_whenEmailNotFound() {
            given(userRepository.findByEmail("nope@test.com")).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.requestDeleteAccountOtp("nope@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("throws IllegalStateException when account already deactivated")
        void shouldThrow_whenAccountDeactivated() {
            User user = buildUser(1L, "dead@test.com", User.Role.PATIENT);
            user.setIsActive(false);
            given(userRepository.findByEmail("dead@test.com")).willReturn(Optional.of(user));
 
            assertThatThrownBy(() -> authService.requestDeleteAccountOtp("dead@test.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Account is already deactivated");
        }
 
        @Test
        @DisplayName("calls OtpService.generateAndSendOtp for active account")
        void shouldCallOtpService_forActiveAccount() {
            User user = buildUser(1L, "active@test.com", User.Role.PATIENT);
            given(userRepository.findByEmail("active@test.com")).willReturn(Optional.of(user));
 
            authService.requestDeleteAccountOtp("active@test.com");
 
            verify(otpService).generateAndSendOtp("active@test.com");
        }
    }
 
    // ── deleteOwnAccountWithOtp ───────────────────────────────────────────────
 
    @Nested
    @DisplayName("deleteOwnAccountWithOtp()")
    class DeleteOwnAccountTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when email not found")
        void shouldThrow_whenEmailNotFound() {
            given(userRepository.findByEmail("x@test.com")).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.deleteOwnAccountWithOtp("x@test.com", "111111"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("throws IllegalArgumentException when OTP is invalid")
        void shouldThrow_whenOtpInvalid() {
            User user = buildUser(1L, "u@test.com", User.Role.PATIENT);
            given(userRepository.findByEmail("u@test.com")).willReturn(Optional.of(user));
            given(otpService.verifyOtp("u@test.com", "000000")).willReturn(false);
 
            assertThatThrownBy(() -> authService.deleteOwnAccountWithOtp("u@test.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid or expired OTP");
 
            verify(userRepository, never()).delete(any());
        }
 
        @Test
        @DisplayName("deletes user and cleans Redis when OTP is valid")
        void shouldDeleteUserAndCleanRedis_whenOtpValid() {
            User user = buildUser(1L, "del@test.com", User.Role.PATIENT);
            given(userRepository.findByEmail("del@test.com")).willReturn(Optional.of(user));
            given(otpService.verifyOtp("del@test.com", "482910")).willReturn(true);
 
            authService.deleteOwnAccountWithOtp("del@test.com", "482910");
 
            verify(userRepository).delete(user);
            verify(redisTemplate).delete("otp:del@test.com");
            verify(redisTemplate).delete("email_verified:del@test.com");
        }
    }
 
    // ── adminDeleteUser ───────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("adminDeleteUser()")
    class AdminDeleteUserTests {
 
        @Test
        @DisplayName("throws ResourceNotFoundException when userId not found")
        void shouldThrow_whenNotFound() {
            given(userRepository.findById(404L)).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> authService.adminDeleteUser(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
 
        @Test
        @DisplayName("throws IllegalStateException when trying to delete an ADMIN account")
        void shouldThrow_whenTargetIsAdmin() {
            User admin = buildUser(10L, "admin@test.com", User.Role.ADMIN);
            given(userRepository.findById(10L)).willReturn(Optional.of(admin));
 
            assertThatThrownBy(() -> authService.adminDeleteUser(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Admin accounts cannot be deleted through this endpoint");
 
            verify(userRepository, never()).delete(any());
        }
 
        @Test
        @DisplayName("deletes PATIENT account and cleans Redis")
        void shouldDeletePatient_andCleanRedis() {
            User patient = buildUser(2L, "patient@test.com", User.Role.PATIENT);
            given(userRepository.findById(2L)).willReturn(Optional.of(patient));
 
            authService.adminDeleteUser(2L);
 
            verify(userRepository).delete(patient);
            verify(redisTemplate).delete("otp:patient@test.com");
            verify(redisTemplate).delete("email_verified:patient@test.com");
        }
 
        @Test
        @DisplayName("deletes PROVIDER account and cleans Redis")
        void shouldDeleteProvider_andCleanRedis() {
            User provider = buildUser(3L, "doc@test.com", User.Role.PROVIDER);
            given(userRepository.findById(3L)).willReturn(Optional.of(provider));
 
            authService.adminDeleteUser(3L);
 
            verify(userRepository).delete(provider);
            verify(redisTemplate).delete("otp:doc@test.com");
        }
    }
 
    // ── logout ────────────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("logout()")
    class LogoutTests {
 
        @Test
        @DisplayName("does not throw any exception")
        void shouldNotThrow() {
            assertThatCode(() -> authService.logout("any.token"))
                    .doesNotThrowAnyException();
        }
    }
}