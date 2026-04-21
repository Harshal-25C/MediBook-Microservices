package com.medibook.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.UnauthorizedException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;

/**
 * Unit tests for AuthServiceImpl.
 *
 * Uses Mockito to mock UserRepository, PasswordEncoder, and JwtUtil
 * so no database or Spring context is needed — tests run in milliseconds.
 *
 * Coverage targets:
 *   register()  — happy path + duplicate email
 *   login()     — happy path + wrong password + inactive account + not found
 *   updateProfile() — happy path + user not found
 *   changePassword() — happy path + wrong current password
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository      userRepository;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtUtil             jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .fullName("Riya Sharma")
                .email("riya@medibook.com")
                .passwordHash("$2a$hashed")
                .phone("9876543210")
                .role("Patient")
                .isActive(true)
                .build();
    }

    /* ── register() ─────────────────────────────────────────── */

    @Test
    @DisplayName("register: success — new user saved and returned")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Riya Sharma");
        req.setEmail("riya@medibook.com");
        req.setPassword("Password@123");
        req.setPhone("9876543210");
        req.setRole("Patient");

        when(userRepository.existsByEmail("riya@medibook.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("riya@medibook.com");
        assertThat(result.getRole()).isEqualTo("Patient");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException for existing email")
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("riya@medibook.com");
        req.setPassword("Password@123");

        when(userRepository.existsByEmail("riya@medibook.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("riya@medibook.com");

        verify(userRepository, never()).save(any());
    }

    /* ── login() ─────────────────────────────────────────────── */

    @Test
    @DisplayName("login: success — returns AuthResponse with JWT token")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("riya@medibook.com");
        req.setPassword("Password@123");

        when(userRepository.findByEmail("riya@medibook.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password@123", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("riya@medibook.com", "Patient", 1))
                .thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(req);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRole()).isEqualTo("Patient");
    }

    @Test
    @DisplayName("login: throws UnauthorizedException for wrong password")
    void login_wrongPassword_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("riya@medibook.com");
        req.setPassword("WrongPassword");

        when(userRepository.findByEmail("riya@medibook.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login: throws UnauthorizedException for inactive account")
    void login_inactiveAccount_throwsException() {
        sampleUser.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("riya@medibook.com");
        req.setPassword("Password@123");

        when(userRepository.findByEmail("riya@medibook.com"))
                .thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("login: throws ResourceNotFoundException for unknown email")
    void login_emailNotFound_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@medibook.com");
        req.setPassword("Password@123");

        when(userRepository.findByEmail("ghost@medibook.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── updateProfile() ────────────────────────────────────── */

    @Test
    @DisplayName("updateProfile: success — fields updated and saved")
    void updateProfile_success() {
        User update = User.builder()
                .fullName("Riya S.")
                .phone("1111111111")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = authService.updateProfile(1, update);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: throws ResourceNotFoundException for unknown userId")
    void updateProfile_userNotFound_throwsException() {
        User update = User.builder().fullName("Ghost").build();
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateProfile(999, update))
                .isInstanceOf(ResourceNotFoundException.class);
    }

//    /* ── changePassword() ───────────────────────────────────── */
//
//    @Test
//    @DisplayName("changePassword: success — password encoded and saved")
//    void changePassword_success() {
//        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches("OldPass@1", "$2a$hashed")).thenReturn(true);
//        when(passwordEncoder.encode("NewPass@1")).thenReturn("$2a$newHash");
//
//      //  authService.changePassword(1, "OldPass@1", "NewPass@1");
//
//        verify(passwordEncoder).encode("NewPass@1");
//        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("$2a$newHash")));
//    }
//
//    @Test
//    @DisplayName("changePassword: throws BadRequestException for wrong current password")
//    void changePassword_wrongCurrentPassword_throwsException() {
//        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches("WrongOld", "$2a$hashed")).thenReturn(false);
//
//      //  assertThatThrownBy(() -> authService.changePassword(1, "WrongOld", "NewPass@1"))
//                .isInstanceOf(BadRequestException.class)
//                .hasMessageContaining("current password");
//    }

    /* ── deactivateAccount() ────────────────────────────────── */

    @Test
    @DisplayName("deactivateAccount: sets isActive=false and saves")
    void deactivateAccount_success() {
    	when(userRepository.findById(anyInt())).thenReturn(Optional.of(sampleUser));
    	when(userRepository.save(any(User.class))).thenReturn(sampleUser);

    	authService.deactivateAccount(1);

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }
}