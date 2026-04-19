package com.medibook.auth.resource;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.auth.config.JwtAuthenticationFilter;
import com.medibook.auth.config.SecurityConfig;
import com.medibook.auth.dto.request.*;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.dto.response.UserResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
 
import java.time.LocalDateTime;
 
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@WebMvcTest(AuthResource.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("AuthResource — MockMvc Tests")
class AuthResourceTest {
 
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
 
    @MockBean AuthService authService;
 
    // ── Needed by SecurityConfig / JwtAuthenticationFilter ───────────────────
    @MockBean com.medibook.auth.util.JwtUtil jwtUtil;
    @MockBean com.medibook.auth.config.CustomUserDetailsService customUserDetailsService;
 
    private static final String BASE = "/api/v1/auth";
 
    // ── Helper builders ───────────────────────────────────────────────────────
 
    private UserResponse sampleUserResponse(Long id, String email, String role) {
        return UserResponse.builder()
                .userId(id)
                .fullName("Test User")
                .email(email)
                .phone("9876543210")
                .role(role)
                .isActive(true)
                .profilePicUrl(null)
                .createdAt(LocalDateTime.now())
                .build();
    }
 
    private AuthResponse sampleAuthResponse(String email, String role) {
        return AuthResponse.builder()
                .token("mocked.jwt.token")
                .tokenType("Bearer")
                .email(email)
                .role(role)
                .userId(1L)
                .build();
    }
 
    // ── POST /send-otp ────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /send-otp")
    class SendOtpTests {
 
        @Test
        @DisplayName("200 OK — valid email sends OTP successfully")
        void shouldReturn200_forValidEmail() throws Exception {
            doNothing().when(authService).sendRegistrationOtp("user@gmail.com");
 
            mockMvc.perform(post(BASE + "/send-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@gmail.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "OTP sent to user@gmail.com. Valid for 10 minutes."));
        }
 
        @Test
        @DisplayName("400 Bad Request — invalid email format")
        void shouldReturn400_forInvalidEmailFormat() throws Exception {
            mockMvc.perform(post(BASE + "/send-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\"}"))
                    .andExpect(status().isBadRequest());
        }
 
        @Test
        @DisplayName("400 Bad Request — email already registered")
        void shouldReturn400_whenEmailAlreadyRegistered() throws Exception {
            doThrow(new IllegalArgumentException("Email is already registered."))
                    .when(authService).sendRegistrationOtp("existing@gmail.com");
 
            mockMvc.perform(post(BASE + "/send-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"existing@gmail.com\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email is already registered."));
        }
    }
 
    // ── POST /verify-otp ──────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /verify-otp")
    class VerifyOtpTests {
 
        @Test
        @DisplayName("200 OK with verified=true for valid OTP")
        void shouldReturn200_verifiedTrue_forValidOtp() throws Exception {
            given(authService.verifyRegistrationOtp("user@gmail.com", "482910")).willReturn(true);
 
            mockMvc.perform(post(BASE + "/verify-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@gmail.com\",\"otp\":\"482910\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true));
        }
 
        @Test
        @DisplayName("400 Bad Request with verified=false for invalid OTP")
        void shouldReturn400_verifiedFalse_forInvalidOtp() throws Exception {
            given(authService.verifyRegistrationOtp("user@gmail.com", "000000")).willReturn(false);
 
            mockMvc.perform(post(BASE + "/verify-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@gmail.com\",\"otp\":\"000000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.verified").value(false));
        }
 
        @Test
        @DisplayName("400 Bad Request — OTP length not exactly 6")
        void shouldReturn400_whenOtpNot6Digits() throws Exception {
            mockMvc.perform(post(BASE + "/verify-otp")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@gmail.com\",\"otp\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
 
    // ── POST /register ────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /register")
    class RegisterTests {
 
        private String patientJson(String email) {
            return String.format(
                    "{\"fullName\":\"Rahul Sharma\",\"email\":\"%s\"," +
                    "\"password\":\"Rahul@123\",\"phone\":\"9876543210\",\"role\":\"PATIENT\"}",
                    email);
        }
 
        @Test
        @DisplayName("201 Created — successful patient registration")
        void shouldReturn201_forSuccessfulRegistration() throws Exception {
            given(authService.register(any(RegisterRequest.class)))
                    .willReturn(sampleUserResponse(1L, "new@gmail.com", "PATIENT"));
 
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patientJson("new@gmail.com")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.role").value("PATIENT"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }
 
        @Test
        @DisplayName("409 Conflict — email not verified")
        void shouldReturn409_whenEmailNotVerified() throws Exception {
            doThrow(new IllegalStateException("Email not verified. Please verify your email with OTP first."))
                    .when(authService).register(any(RegisterRequest.class));
 
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patientJson("unverified@gmail.com")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            "Email not verified. Please verify your email with OTP first."));
        }
 
        @Test
        @DisplayName("400 Bad Request — password too short")
        void shouldReturn400_whenPasswordTooShort() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\",\"email\":\"t@t.com\"," +
                                     "\"password\":\"123\",\"phone\":\"9876543210\",\"role\":\"PATIENT\"}"))
                    .andExpect(status().isBadRequest());
        }
 
        @Test
        @DisplayName("400 Bad Request — invalid role value")
        void shouldReturn400_whenRoleIsInvalid() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\",\"email\":\"t@t.com\"," +
                                     "\"password\":\"Test@1234\",\"phone\":\"9876543210\",\"role\":\"INVALID_ROLE\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
 
    // ── POST /login ───────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /login")
    class LoginTests {
 
        @Test
        @DisplayName("200 OK — returns token on valid credentials")
        void shouldReturn200_withToken_onValidCredentials() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willReturn(sampleAuthResponse("admin@gmail.com", "ADMIN"));
 
            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@gmail.com\",\"password\":\"Admin@123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
 
        @Test
        @DisplayName("400 Bad Request — wrong password")
        void shouldReturn400_onWrongPassword() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new IllegalArgumentException("Invalid credentials"));
 
            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@gmail.com\",\"password\":\"wrong\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }
 
        @Test
        @DisplayName("409 Conflict — deactivated account")
        void shouldReturn409_forDeactivatedAccount() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new IllegalStateException("Account is deactivated"));
 
            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"dead@gmail.com\",\"password\":\"pass\"}"))
                    .andExpect(status().isConflict());
        }
 
        @Test
        @DisplayName("404 Not Found — user does not exist")
        void shouldReturn404_whenUserNotFound() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new ResourceNotFoundException("User not found"));
 
            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ghost@gmail.com\",\"password\":\"pass\"}"))
                    .andExpect(status().isNotFound());
        }
    }
 
    // ── GET /validate ─────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("GET /validate")
    class ValidateTokenTests {
 
        @Test
        @DisplayName("200 OK — returns valid=true for good token")
        void shouldReturn200_withValidTrue() throws Exception {
            given(authService.validateToken("good.token")).willReturn(true);
 
            mockMvc.perform(get(BASE + "/validate")
                            .header("Authorization", "Bearer good.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }
 
        @Test
        @DisplayName("200 OK — returns valid=false for bad token")
        void shouldReturn200_withValidFalse() throws Exception {
            given(authService.validateToken("bad.token")).willReturn(false);
 
            mockMvc.perform(get(BASE + "/validate")
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }
    }
 
    // ── GET /profile/{userId} ─────────────────────────────────────────────────
 
    @Nested
    @DisplayName("GET /profile/{userId}")
    class GetProfileTests {
 
        @Test
        @WithMockUser(username = "user@gmail.com", roles = {"PATIENT"})
        @DisplayName("200 OK — returns profile for valid userId")
        void shouldReturn200_withProfile() throws Exception {
            given(authService.getUserById(1L))
                    .willReturn(sampleUserResponse(1L, "user@gmail.com", "PATIENT"));
 
            mockMvc.perform(get(BASE + "/profile/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.email").value("user@gmail.com"));
        }
 
        @Test
        @WithMockUser(roles = {"PATIENT"})
        @DisplayName("404 Not Found — userId does not exist")
        void shouldReturn404_whenUserNotFound() throws Exception {
            given(authService.getUserById(999L))
                    .willThrow(new ResourceNotFoundException("User not found: 999"));
 
            mockMvc.perform(get(BASE + "/profile/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found: 999"));
        }
 
        @Test
        @DisplayName("403 Forbidden — no token provided")
        void shouldReturn403_whenNoToken() throws Exception {
            mockMvc.perform(get(BASE + "/profile/1"))
                    .andExpect(status().isForbidden());
        }
    }
 
    // ── PUT /profile/{userId} ─────────────────────────────────────────────────
 
    @Nested
    @DisplayName("PUT /profile/{userId}")
    class UpdateProfileTests {
 
        @Test
        @WithMockUser(username = "user@gmail.com", roles = {"PATIENT"})
        @DisplayName("200 OK — updates profile successfully")
        void shouldReturn200_afterUpdate() throws Exception {
            UserResponse updated = sampleUserResponse(1L, "user@gmail.com", "PATIENT");
            updated.setFullName("Updated Name");
            given(authService.updateProfile(eq(1L), any(UpdateProfileRequest.class)))
                    .willReturn(updated);
 
            mockMvc.perform(put(BASE + "/profile/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Updated Name\"}"))
                    .andExpect(status().isOk());
        }
    }
 
    // ── PUT /password/{userId} ────────────────────────────────────────────────
 
    @Nested
    @DisplayName("PUT /password/{userId}")
    class ChangePasswordTests {
 
        @Test
        @WithMockUser(roles = {"PATIENT"})
        @DisplayName("204 No Content — password changed successfully")
        void shouldReturn204_afterPasswordChange() throws Exception {
            doNothing().when(authService).changePassword(1L, "OldPass@1", "NewPass@2");
 
            mockMvc.perform(put(BASE + "/password/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"OldPass@1\",\"newPassword\":\"NewPass@2\"}"))
                    .andExpect(status().isNoContent());
        }
 
        @Test
        @WithMockUser(roles = {"PATIENT"})
        @DisplayName("400 Bad Request — wrong old password")
        void shouldReturn400_whenOldPasswordWrong() throws Exception {
            doThrow(new IllegalArgumentException("Current password is incorrect"))
                    .when(authService).changePassword(anyLong(), anyString(), anyString());
 
            mockMvc.perform(put(BASE + "/password/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"NewPass@2\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
 
    // ── PUT /deactivate/{userId} ──────────────────────────────────────────────
 
    @Nested
    @DisplayName("PUT /deactivate/{userId}")
    class DeactivateTests {
 
        @Test
        @WithMockUser(roles = {"PATIENT"})
        @DisplayName("204 No Content — account deactivated")
        void shouldReturn204_afterDeactivation() throws Exception {
            doNothing().when(authService).deactivateAccount(1L);
 
            mockMvc.perform(put(BASE + "/deactivate/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
 
    // ── POST /refresh ─────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /refresh")
    class RefreshTokenTests {
 
        @Test
        @DisplayName("200 OK — returns new token")
        void shouldReturn200_withNewToken() throws Exception {
            given(authService.refreshToken("old.token")).willReturn("new.jwt.token");
 
            mockMvc.perform(post(BASE + "/refresh")
                            .with(csrf())
                            .header("Authorization", "Bearer old.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("new.jwt.token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }
 
        @Test
        @DisplayName("400 Bad Request — invalid old token")
        void shouldReturn400_forInvalidOldToken() throws Exception {
            given(authService.refreshToken("bad.token"))
                    .willThrow(new IllegalArgumentException("Invalid or expired token"));
 
            mockMvc.perform(post(BASE + "/refresh")
                            .with(csrf())
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isBadRequest());
        }
    }
 
    // ── POST /logout ──────────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("POST /logout")
    class LogoutTests {
 
        @Test
        @WithMockUser(roles = {"PATIENT"})
        @DisplayName("204 No Content — logout successful")
        void shouldReturn204_afterLogout() throws Exception {
            doNothing().when(authService).logout(anyString());
 
            mockMvc.perform(post(BASE + "/logout")
                            .with(csrf())
                            .header("Authorization", "Bearer some.valid.token"))
                    .andExpect(status().isNoContent());
        }
    }
 
    // ── DELETE /admin/users/{userId} ──────────────────────────────────────────
 
    @Nested
    @DisplayName("DELETE /admin/users/{userId}")
    class AdminDeleteUserTests {
 
        @Test
        @WithMockUser(username = "admin@gmail.com", roles = {"ADMIN"})
        @DisplayName("200 OK — admin deletes user successfully")
        void shouldReturn200_whenAdminDeletesUser() throws Exception {
            doNothing().when(authService).adminDeleteUser(2L);
 
            mockMvc.perform(delete(BASE + "/admin/users/2").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("User deleted successfully by admin."));
        }
 
        @Test
        @WithMockUser(username = "patient@gmail.com", roles = {"PATIENT"})
        @DisplayName("403 Forbidden — non-admin cannot use admin endpoint")
        void shouldReturn403_whenNonAdminCallsAdminEndpoint() throws Exception {
            mockMvc.perform(delete(BASE + "/admin/users/2").with(csrf()))
                    .andExpect(status().isForbidden());
        }
 
        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("409 Conflict — cannot delete another admin account")
        void shouldReturn409_whenDeletingAdminAccount() throws Exception {
            doThrow(new IllegalStateException(
                    "Admin accounts cannot be deleted through this endpoint"))
                    .when(authService).adminDeleteUser(10L);
 
            mockMvc.perform(delete(BASE + "/admin/users/10").with(csrf()))
                    .andExpect(status().isConflict());
        }
 
        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("404 Not Found — userId does not exist")
        void shouldReturn404_whenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: 404"))
                    .when(authService).adminDeleteUser(404L);
 
            mockMvc.perform(delete(BASE + "/admin/users/404").with(csrf()))
                    .andExpect(status().isNotFound());
        }
 
        @Test
        @DisplayName("403 Forbidden — no authentication at all")
        void shouldReturn403_whenNoAuth() throws Exception {
            mockMvc.perform(delete(BASE + "/admin/users/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}