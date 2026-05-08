package com.medibook.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .fullName("Riya Sharma")
                .email("riya@medibook.com")
                .passwordHash("hash")
                .phone("9876543210")
                .role("Patient")
                .isActive(true)
                .build();
    }

    @Test
    void register_savesNewActiveUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Riya Sharma");
        request.setEmail("riya@medibook.com");
        request.setPassword("secret123");
        request.setPhone("9876543210");
        request.setRole("Patient");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = authService.register(request);

        assertThat(saved.getEmail()).isEqualTo("riya@medibook.com");
        verify(userRepository).save(argThat(u -> u.isActive() && u.getPasswordHash().equals("hash")));
    }

    @Test
    void register_duplicateEmailThrows() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("riya@medibook.com");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentialsReturnsJwt() {
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("secret123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId())).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("Patient");
    }

    @Test
    void login_rejectsInactiveAndBadPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("bad");
        user.setActive(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);

        user.setActive(true);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void userLookupsAndProfileUpdatesUseRepository() {
        User update = User.builder().fullName("Riya S").phone("111").profilePicUrl("pic.png").build();
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.updateProfile(1, update);

        assertThat(result.getFullName()).isEqualTo("Riya S");
        verify(userRepository).save(argThat(u -> "111".equals(u.getPhone())));
        clearInvocations(userRepository);

        authService.deactivateAccount(1);
        assertThat(user.isActive()).isFalse();
        clearInvocations(userRepository);

        authService.reactivateAccount(1);
        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(argThat(User::isActive));
    }

    @Test
    void changePasswordValidatesAndSavesEncodedPassword() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");

        authService.changePassword(1, "newpass");

        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("new-hash")));
        assertThatThrownBy(() -> authService.changePassword(1, " "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.changePassword(1, "123"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void tokenAndOtpOperationsDelegateToCollaborators() {
        when(jwtUtil.validateToken("old")).thenReturn(true);
        when(jwtUtil.extractEmail("old")).thenReturn(user.getEmail());
        when(jwtUtil.extractRole("old")).thenReturn(user.getRole());
        when(jwtUtil.extractUserId("old")).thenReturn(user.getUserId());
        when(jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId())).thenReturn("new");
        when(otpService.verifyOtp(user.getEmail(), "123456")).thenReturn(true);

        assertThat(authService.validateToken("old")).isTrue();
        assertThat(authService.refreshToken("old")).isEqualTo("new");
        authService.sendOtp(user.getEmail());
        assertThat(authService.verifyOtp(user.getEmail(), "123456")).isTrue();

        verify(otpService).generateAndSendOtp(user.getEmail());

        when(jwtUtil.validateToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> authService.refreshToken("bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void registerAdminAndGoogleUserCoverRolePaths() {
        RegisterAdminRequest adminRequest = new RegisterAdminRequest();
        adminRequest.setFullName("Admin");
        adminRequest.setEmail("admin@medibook.com");
        adminRequest.setPassword("secret123");
        adminRequest.setAdminCode("CODE");
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User admin = authService.registerAdmin(adminRequest, "CODE");
        assertThat(admin.getRole()).isEqualTo("Admin");

        adminRequest.setAdminCode("WRONG");
        assertThatThrownBy(() -> authService.registerAdmin(adminRequest, "CODE"))
                .isInstanceOf(RuntimeException.class);
        adminRequest.setAdminCode("CODE");
        when(userRepository.existsByEmail(adminRequest.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> authService.registerAdmin(adminRequest, "CODE"))
                .isInstanceOf(RuntimeException.class);

        when(userRepository.findByEmail("google@medibook.com")).thenReturn(Optional.empty());
        User googleUser = authService.findOrCreateGoogleUser("google@medibook.com", "G User", "pic", "google", "Provider");
        assertThat(googleUser.getProvider()).isEqualTo("google");

        assertThatThrownBy(() -> authService.findOrCreateGoogleUser("x@y.com", "X", null, "google", "Admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPasswordFlowValidatesTokenAndUpdatesPassword() {
        PasswordResetToken token = PasswordResetToken.builder()
                .email(user.getEmail())
                .token("reset-token")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");

        authService.verifyResetOtp("reset-token", "123456");
        authService.resetPassword("reset-token", "newpass");

        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("new-hash")));
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void forgotPasswordCreatesResetTokenAndToleratesMailFailure() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        authService.forgotPassword(user.getEmail());

        verify(passwordResetTokenRepository).deleteAllByEmail(user.getEmail());
        verify(passwordResetTokenRepository).save(argThat(t -> t.getEmail().equals(user.getEmail()) && t.getOtp().length() == 6));

        when(userRepository.findByEmail("missing@medibook.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.forgotPassword("missing@medibook.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetOtpAndResetPasswordValidationBranchesThrow() {
        PasswordResetToken used = PasswordResetToken.builder()
                .email(user.getEmail()).token("used").otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).used(true).build();
        PasswordResetToken expired = PasswordResetToken.builder()
                .email(user.getEmail()).token("expired").otp("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1)).used(false).build();
        PasswordResetToken valid = PasswordResetToken.builder()
                .email(user.getEmail()).token("valid").otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).used(false).build();

        when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(used));
        when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));
        when(passwordResetTokenRepository.findByToken("valid")).thenReturn(Optional.of(valid));

        assertThatThrownBy(() -> authService.verifyResetOtp("missing", "123456")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.verifyResetOtp("used", "123456")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.verifyResetOtp("expired", "123456")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.verifyResetOtp("valid", "000000")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.resetPassword("valid", " ")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.resetPassword("valid", "123")).isInstanceOf(BadRequestException.class);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.resetPassword("valid", "newpass"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void queryMethodsReturnRepositoryResultsAndNotFoundThrows() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(404)).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findAllByRole("Patient")).thenReturn(List.of(user));

        assertThat(authService.getUserByEmail(user.getEmail())).isSameAs(user);
        assertThat(authService.getAllUsers()).containsExactly(user);
        assertThat(authService.getUsersByRole("Patient")).containsExactly(user);
        assertThatThrownBy(() -> authService.getUserById(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
