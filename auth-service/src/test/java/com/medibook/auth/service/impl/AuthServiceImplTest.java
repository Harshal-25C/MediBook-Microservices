package com.medibook.auth.service.impl;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
 
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.PasswordResetToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.UnauthorizedException;
import com.medibook.auth.repository.PasswordResetTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.otp.service.OtpService;
 
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
 
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private OtpService otpService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JavaMailSender mailSender;
 
    private AuthServiceImpl authService;
 
    private User activeUser;
    private User inactiveUser;
 
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl();
        // inject mocks via reflection since @Autowired fields
        injectField(authService, "userRepository", userRepository);
        injectField(authService, "passwordEncoder", passwordEncoder);
        injectField(authService, "jwtUtil", jwtUtil);
        injectField(authService, "otpService", otpService);
        injectField(authService, "passwordResetTokenRepository", passwordResetTokenRepository);
        injectField(authService, "mailSender", mailSender);
 
        activeUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@medibook.com")
                .passwordHash("hashedPwd")
                .phone("9876543210")
                .role("Patient")
                .isActive(true)
                .build();
 
        inactiveUser = User.builder()
                .userId(2)
                .fullName("Jane Inactive")
                .email("jane@medibook.com")
                .passwordHash("hashedPwd")
                .role("Patient")
                .isActive(false)
                .build();
    }
 
    // ── Helpers ──────────────────────────────────────────────────────────
 
    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
 
    // ─────────────────────────── register ────────────────────────────────
 
    @Test
    void register_success_savesUserWithEncodedPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Alice"); req.setEmail("alice@x.com");
        req.setPassword("pwd"); req.setPhone("111"); req.setRole("Patient");
 
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(passwordEncoder.encode("pwd")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
 
        User result = authService.register(req);
 
        assertThat(result.getEmail()).isEqualTo("alice@x.com");
        assertThat(result.getPasswordHash()).isEqualTo("encoded");
        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(any(User.class));
    }
 
    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@medibook.com");
 
        when(userRepository.existsByEmail("john@medibook.com")).thenReturn(true);
 
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }
 
    // ─────────────────────────── login ───────────────────────────────────
 
    @Test
    void login_success_returnsAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@medibook.com"); req.setPassword("plainPwd");
 
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPwd", "hashedPwd")).thenReturn(true);
        when(jwtUtil.generateToken("john@medibook.com", "Patient", 1)).thenReturn("jwt-token");
 
        AuthResponse resp = authService.login(req);
 
        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getRole()).isEqualTo("Patient");
        assertThat(resp.getUserId()).isEqualTo(1);
    }
 
    @Test
    void login_userNotFound_throwsResourceNotFoundException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@x.com");
 
        when(userRepository.findByEmail("unknown@x.com")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    @Test
    void login_inactiveUser_throwsUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("jane@medibook.com"); req.setPassword("pwd");
 
        when(userRepository.findByEmail("jane@medibook.com")).thenReturn(Optional.of(inactiveUser));
 
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }
 
    @Test
    void login_wrongPassword_throwsUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@medibook.com"); req.setPassword("wrong");
 
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "hashedPwd")).thenReturn(false);
 
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }
 
    // ─────────────────────────── registerAdmin ───────────────────────────
 
    @Test
    void registerAdmin_validCode_createsAdminUser() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("Admin One"); req.setEmail("admin@x.com");
        req.setPassword("secure"); req.setAdminCode("SECRET");
 
        when(userRepository.existsByEmail("admin@x.com")).thenReturn(false);
        when(passwordEncoder.encode("secure")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
 
        User result = authService.registerAdmin(req, "SECRET");
 
        assertThat(result.getRole()).isEqualTo("Admin");
        assertThat(result.isActive()).isTrue();
    }
 
    @Test
    void registerAdmin_wrongCode_throwsRuntimeException() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setAdminCode("WRONG");
 
        assertThatThrownBy(() -> authService.registerAdmin(req, "SECRET"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid admin code");
    }
 
    @Test
    void registerAdmin_duplicateEmail_throwsRuntimeException() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setAdminCode("SECRET"); req.setEmail("dup@x.com");
 
        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);
 
        assertThatThrownBy(() -> authService.registerAdmin(req, "SECRET"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }
 
    // ─────────────────────────── findOrCreateGoogleUser ──────────────────
 
    @Test
    void findOrCreateGoogleUser_existingUser_returnsWithoutCreating() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
 
        User result = authService.findOrCreateGoogleUser("john@medibook.com", "John", null, "google", "Patient");
 
        assertThat(result.getUserId()).isEqualTo(1);
        verify(userRepository, never()).save(any());
    }
 
    @Test
    void findOrCreateGoogleUser_newUser_validRole_saves() {
        when(userRepository.findByEmail("new@g.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
 
        User result = authService.findOrCreateGoogleUser("new@g.com", "New User", "pic.jpg", "google", "Provider");
 
        assertThat(result.getRole()).isEqualTo("Provider");
        assertThat(result.getProvider()).isEqualTo("google");
        verify(userRepository).save(any(User.class));
    }
 
    @Test
    void findOrCreateGoogleUser_invalidRole_throwsBadRequestException() {
        when(userRepository.findByEmail("x@g.com")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> authService.findOrCreateGoogleUser("x@g.com", "X", null, "google", "Admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role");
    }
 
    // ─────────────────────────── logout / validateToken / refreshToken ───
 
    @Test
    void logout_doesNotThrow() {
        assertThatCode(() -> authService.logout("some-token")).doesNotThrowAnyException();
    }
 
    @Test
    void validateToken_delegatesToJwtUtil() {
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        assertThat(authService.validateToken("tok")).isTrue();
    }
 
    @Test
    void refreshToken_validToken_returnsNewToken() {
        when(jwtUtil.validateToken("old")).thenReturn(true);
        when(jwtUtil.extractEmail("old")).thenReturn("john@medibook.com");
        when(jwtUtil.extractRole("old")).thenReturn("Patient");
        when(jwtUtil.extractUserId("old")).thenReturn(1);
        when(jwtUtil.generateToken("john@medibook.com", "Patient", 1)).thenReturn("new-tok");
 
        assertThat(authService.refreshToken("old")).isEqualTo("new-tok");
    }
 
    @Test
    void refreshToken_invalidToken_throwsUnauthorizedException() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);
 
        assertThatThrownBy(() -> authService.refreshToken("bad"))
                .isInstanceOf(UnauthorizedException.class);
    }
 
    // ─────────────────────────── getUserByEmail / getUserById ────────────
 
    @Test
    void getUserByEmail_found_returnsUser() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        assertThat(authService.getUserByEmail("john@medibook.com").getFullName()).isEqualTo("John Doe");
    }
 
    @Test
    void getUserByEmail_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getUserByEmail("x@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    @Test
    void getUserById_found_returnsUser() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        assertThat(authService.getUserById(1).getEmail()).isEqualTo("john@medibook.com");
    }
 
    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getUserById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── updateProfile ───────────────────────────
 
    @Test
    void updateProfile_updatesFieldsAndSaves() {
        User updated = User.builder().fullName("New Name").phone("000").profilePicUrl("url").build();
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        User result = authService.updateProfile(1, updated);
 
        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("000");
        assertThat(result.getProfilePicUrl()).isEqualTo("url");
    }
 
    // ─────────────────────────── changePassword ──────────────────────────
 
    @Test
    void changePassword_valid_encodesAndSaves() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");
        when(userRepository.save(any())).thenReturn(activeUser);
 
        authService.changePassword(1, "newPass123");
 
        verify(userRepository).save(argThat(u -> "newHash".equals(u.getPasswordHash())));
    }
 
    @Test
    void changePassword_nullPassword_throwsBadRequestException() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        assertThatThrownBy(() -> authService.changePassword(1, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }
 
    @Test
    void changePassword_blankPassword_throwsBadRequestException() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        assertThatThrownBy(() -> authService.changePassword(1, "   "))
                .isInstanceOf(BadRequestException.class);
    }
 
    @Test
    void changePassword_tooShort_throwsBadRequestException() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        assertThatThrownBy(() -> authService.changePassword(1, "abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("6 characters");
    }
 
    // ─────────────────────────── deactivate / reactivate ─────────────────
 
    @Test
    void deactivateAccount_setsActiveFalse() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);
 
        authService.deactivateAccount(1);
 
        assertThat(activeUser.isActive()).isFalse();
        verify(userRepository).save(activeUser);
    }
 
    @Test
    void reactivateAccount_setsActiveTrue() {
        inactiveUser.setActive(false);
        when(userRepository.findByUserId(2)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any())).thenReturn(inactiveUser);
 
        authService.reactivateAccount(2);
 
        assertThat(inactiveUser.isActive()).isTrue();
        verify(userRepository).save(inactiveUser);
    }
 
    // ─────────────────────────── getAllUsers / getUsersByRole ─────────────
 
    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));
        assertThat(authService.getAllUsers()).hasSize(2);
    }
 
    @Test
    void getUsersByRole_filtersCorrectly() {
        when(userRepository.findAllByRole("Admin")).thenReturn(List.of());
        assertThat(authService.getUsersByRole("Admin")).isEmpty();
    }
 
    // ─────────────────────────── sendOtp / verifyOtp ─────────────────────
 
    @Test
    void sendOtp_delegatesToOtpService() {
        authService.sendOtp("john@medibook.com");
        verify(otpService).generateAndSendOtp("john@medibook.com");
    }
 
    @Test
    void verifyOtp_delegatesToOtpService() {
        when(otpService.verifyOtp("john@medibook.com", "123456")).thenReturn(true);
        assertThat(authService.verifyOtp("john@medibook.com", "123456")).isTrue();
    }
 
    // ─────────────────────────── forgotPassword ──────────────────────────
 
    @Test
    void forgotPassword_userExists_savesTokenAndSendsEmail() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(passwordResetTokenRepository).deleteAllByEmail("john@medibook.com");
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
 
        authService.forgotPassword("john@medibook.com");
 
        verify(passwordResetTokenRepository).deleteAllByEmail("john@medibook.com");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }
 
    @Test
    void forgotPassword_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.forgotPassword("x@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    @Test
    void forgotPassword_emailFailure_doesNotThrow() {
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        doNothing().when(passwordResetTokenRepository).deleteAllByEmail(any());
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
 
        // Should NOT throw - email failure is caught internally
        assertThatCode(() -> authService.forgotPassword("john@medibook.com")).doesNotThrowAnyException();
    }
 
    // ─────────────────────────── verifyResetOtp ──────────────────────────
 
    @Test
    void verifyResetOtp_validTokenAndOtp_succeeds() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok123").otp("654321").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
 
        when(passwordResetTokenRepository.findByToken("tok123")).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);
 
        assertThatCode(() -> authService.verifyResetOtp("tok123", "654321")).doesNotThrowAnyException();
    }
 
    @Test
    void verifyResetOtp_invalidToken_throwsBadRequestException() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.verifyResetOtp("bad", "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }
 
    @Test
    void verifyResetOtp_alreadyUsed_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("123456").used(true)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
 
        assertThatThrownBy(() -> authService.verifyResetOtp("tok", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }
 
    @Test
    void verifyResetOtp_expired_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("123456").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
        doNothing().when(passwordResetTokenRepository).delete(resetToken);
 
        assertThatThrownBy(() -> authService.verifyResetOtp("tok", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }
 
    @Test
    void verifyResetOtp_wrongOtp_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("111111").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
 
        assertThatThrownBy(() -> authService.verifyResetOtp("tok", "999999"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }
 
    // ─────────────────────────── resetPassword ───────────────────────────
 
    @Test
    void resetPassword_valid_encodesAndSavesAndDeletesToken() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("111111").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
        when(userRepository.findByEmail("john@medibook.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");
        when(userRepository.save(any())).thenReturn(activeUser);
        doNothing().when(passwordResetTokenRepository).delete(resetToken);
 
        authService.resetPassword("tok", "newPass123");
 
        verify(userRepository).save(argThat(u -> "newHash".equals(u.getPasswordHash())));
        verify(passwordResetTokenRepository).delete(resetToken);
    }
 
    @Test
    void resetPassword_invalidToken_throwsBadRequestException() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.resetPassword("bad", "newPass123"))
                .isInstanceOf(BadRequestException.class);
    }
 
    @Test
    void resetPassword_expired_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("111111").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
        doNothing().when(passwordResetTokenRepository).delete(resetToken);
 
        assertThatThrownBy(() -> authService.resetPassword("tok", "newPass123"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("expired");
    }
 
    @Test
    void resetPassword_emptyPassword_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("111111").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
 
        assertThatThrownBy(() -> authService.resetPassword("tok", ""))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("cannot be empty");
    }
 
    @Test
    void resetPassword_shortPassword_throwsBadRequestException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("tok").otp("111111").used(false)
                .email("john@medibook.com").build();
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
 
        when(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(resetToken));
 
        assertThatThrownBy(() -> authService.resetPassword("tok", "abc"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("6 characters");
    }
}