package com.medibook.auth.resource;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.User;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock private AuthService authService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthResource authResource;

    private User patientUser;
    private User adminUser;
    private User googleUser;

    @BeforeEach
    void setUp() throws Exception {
        // Inject adminSecretCode field value
        Field field = AuthResource.class.getDeclaredField("adminSecretCode");
        field.setAccessible(true);
        field.set(authResource, "ADMIN_SECRET");

        patientUser = User.builder()
                .userId(1).fullName("John Doe")
                .email("john@medibook.com").phone("9876543210")
                .role("Patient").isActive(true).build();

        adminUser = User.builder()
                .userId(10).fullName("Admin User")
                .email("admin@medibook.com").role("Admin").isActive(true).build();

        googleUser = User.builder()
                .userId(5).fullName("Google User")
                .email("google@gmail.com").role("Patient")
                .provider("google").isActive(true).build();
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void register_success_returns201() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Alice"); req.setEmail("alice@x.com");
        req.setPassword("pass123"); req.setRole("Patient");

        when(authService.register(any())).thenReturn(patientUser);

        ResponseEntity<?> response = authResource.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body).containsKey("userId");
        assertThat(body).containsKey("role");
    }

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    void login_withPhone_sendsOtpAndReturns200() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@medibook.com"); req.setPassword("pass");

        when(authService.login(any())).thenReturn(new AuthResponse("tok", "Patient", 1, "John", "ok"));
        when(authService.getUserByEmail("john@medibook.com")).thenReturn(patientUser);
        doNothing().when(authService).sendOtp("john@medibook.com");

        ResponseEntity<?> response = authResource.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("otpSent");
        assertThat(body.get("otpSent")).isEqualTo(true);
    }

    @Test
    void login_adminWithoutPhone_stillSendsOtp() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@medibook.com"); req.setPassword("pass");

        when(authService.login(any())).thenReturn(new AuthResponse("tok", "Admin", 10, "Admin", "ok"));
        when(authService.getUserByEmail("admin@medibook.com")).thenReturn(adminUser);
        doNothing().when(authService).sendOtp("admin@medibook.com");

        ResponseEntity<?> response = authResource.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).sendOtp("admin@medibook.com");
    }

    @Test
    void login_patientWithNoPhone_returns200WithRequiresPhone() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nophone@x.com"); req.setPassword("pass");

        User noPhoneUser = User.builder()
                .userId(3).email("nophone@x.com").role("Patient")
                .phone(null).provider(null).isActive(true).build();

        when(authService.login(any())).thenReturn(new AuthResponse("tok", "Patient", 3, "NP", "ok"));
        when(authService.getUserByEmail("nophone@x.com")).thenReturn(noPhoneUser);

        ResponseEntity<?> response = authResource.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("requiresPhone")).isEqualTo(true);
    }

    // ── verifyOtp ──────────────────────────────────────────────────────────

    @Test
    void verifyOtp_validOtp_returnsTokenAndUserInfo() {
        Map<String, String> body = Map.of(
                "email", "john@medibook.com",
                "otp", "123456"
        );

        when(authService.verifyOtp(anyString(), anyString()))
                .thenReturn(true);

        when(authService.getUserByEmail("john@medibook.com"))
                .thenReturn(patientUser);

        when(jwtUtil.generateToken(
                "john@medibook.com",
                "Patient",
                1))
                .thenReturn("new-jwt");

        ResponseEntity<?> response = authResource.verifyOtp(body);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp =
                (Map<String, Object>) response.getBody();

        assertThat(resp.get("token"))
                .isEqualTo("new-jwt");
        assertThat(resp).containsKey("userId");
        assertThat(resp).containsKey("role");
    }

    // ── resendOtp ──────────────────────────────────────────────────────────

    @Test
    void resendOtp_callsSendOtpAndReturns200() {
        Map<String, String> body = Map.of("email", "john@medibook.com");
        doNothing().when(authService).sendOtp("john@medibook.com");

        ResponseEntity<?> response = authResource.resendOtp(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).sendOtp("john@medibook.com");
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("otpSent")).isEqualTo(true);
    }

    // ── logout ────────────────────────────────────────────────────────────

    @Test
    void logout_callsServiceAndReturns200() {
        doNothing().when(authService).logout("validtoken");

        ResponseEntity<?> response = authResource.logout("Bearer validtoken");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).logout("validtoken");
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Test
    void refresh_returnsNewToken() {
        when(authService.refreshToken("oldtoken")).thenReturn("refreshedToken");

        ResponseEntity<?> response = authResource.refresh("Bearer oldtoken");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("token")).isEqualTo("refreshedToken");
    }

    // ── getProfile ────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsUser() {
        when(authService.getUserById(1)).thenReturn(patientUser);

        ResponseEntity<User> response = authResource.getProfile(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("john@medibook.com");
    }

    // ── updateProfile ─────────────────────────────────────────────────────

    @Test
    void updateProfile_callsServiceAndReturnsUpdatedUser() {
        User updated = User.builder().fullName("New Name").phone("000").build();
        when(authService.updateProfile(1, updated)).thenReturn(patientUser);

        ResponseEntity<User> response = authResource.updateProfile(1, updated);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).updateProfile(1, updated);
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    void changePassword_callsServiceAndReturns200() {
        doNothing().when(authService).changePassword(1, "newPass123");

        ResponseEntity<?> response = authResource.changePassword(1, Map.of("newPassword", "newPass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).changePassword(1, "newPass123");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("message")).isEqualTo("Password updated successfully");
    }

    // ── deactivate ────────────────────────────────────────────────────────

    @Test
    void deactivate_callsServiceAndReturns200() {
        doNothing().when(authService).deactivateAccount(1);

        ResponseEntity<?> response = authResource.deactivate(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).deactivateAccount(1);
    }

    // ── adminRegister ─────────────────────────────────────────────────────

    @Test
    void registerAdmin_validCode_returns201() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("New Admin"); req.setEmail("newadmin@x.com");
        req.setPassword("secure"); req.setAdminCode("ADMIN_SECRET");

        when(authService.registerAdmin(any(), eq("ADMIN_SECRET"))).thenReturn(adminUser);

        ResponseEntity<?> response = authResource.registerAdmin(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("message");
    }

    @Test
    void registerAdmin_serviceThrowsException_returns401() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setAdminCode("WRONG");

        when(authService.registerAdmin(any(), any())).thenThrow(new RuntimeException("Invalid admin code"));

        ResponseEntity<?> response = authResource.registerAdmin(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── googleComplete ────────────────────────────────────────────────────

    @Test
    void completeGoogleLogin_returnsTokenAndUserInfo() {
        Map<String, String> body = Map.of(
                "email", "google@gmail.com", "fullName", "Google User",
                "picture", "pic.jpg", "provider", "google", "role", "Patient"
        );

        when(authService.findOrCreateGoogleUser(any(), any(), any(), any(), any())).thenReturn(googleUser);
        when(jwtUtil.generateToken("google@gmail.com", "Patient", 5)).thenReturn("google-jwt");

        ResponseEntity<?> response = authResource.completeGoogleLogin(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("token")).isEqualTo("google-jwt");
        assertThat(resp).containsKey("userId");
    }

    // ── addPhone ──────────────────────────────────────────────────────────

    @Test
    void addPhone_validPhone_updatesAndSendsOtp() {
        Map<String, String> body = Map.of("email", "john@medibook.com", "phone", "9999999999");

        when(authService.getUserByEmail("john@medibook.com")).thenReturn(patientUser);
        when(authService.updateProfile(anyInt(), any())).thenReturn(patientUser);
        doNothing().when(authService).sendOtp("john@medibook.com");

        ResponseEntity<?> response = authResource.addPhone(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).sendOtp("john@medibook.com");
    }

    @Test
    void addPhone_emptyPhone_returnsBadRequest() {
        Map<String, String> body = Map.of("email", "john@medibook.com", "phone", "");

        ResponseEntity<?> response = authResource.addPhone(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── forgotPassword ────────────────────────────────────────────────────

    @Test
    void forgotPassword_validEmail_returnsSentTrue() {
        Map<String, String> body = Map.of("email", "john@medibook.com");
        doNothing().when(authService).forgotPassword("john@medibook.com");

        ResponseEntity<?> response = authResource.forgotPassword(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("sent")).isEqualTo(true);
    }

    @Test
    void forgotPassword_emptyEmail_returnsBadRequest() {
        Map<String, String> body = Map.of("email", "");

        ResponseEntity<?> response = authResource.forgotPassword(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void forgotPassword_serviceThrows_stillReturnsOkWithGenericMessage() {
        Map<String, String> body = Map.of("email", "notfound@x.com");
        doThrow(new RuntimeException("not found")).when(authService).forgotPassword("notfound@x.com");

        ResponseEntity<?> response = authResource.forgotPassword(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── verifyResetOtp ────────────────────────────────────────────────────

    @Test
    void verifyResetOtp_validTokenAndOtp_returnsVerifiedTrue() {
        Map<String, String> body = Map.of("token", "tok123", "otp", "654321");
        doNothing().when(authService).verifyResetOtp("tok123", "654321");

        ResponseEntity<?> response = authResource.verifyResetOtp(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("verified")).isEqualTo(true);
    }

    // ── resetPassword ─────────────────────────────────────────────────────

    @Test
    void resetPassword_valid_returnsSuccessTrue() {
        Map<String, String> body = Map.of("token", "tok", "newPassword", "newPass123");
        doNothing().when(authService).resetPassword("tok", "newPass123");

        ResponseEntity<?> response = authResource.resetPassword(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("success")).isEqualTo(true);
    }

    // ── getAllUsers ───────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsList() {
        when(authService.getAllUsers()).thenReturn(List.of(patientUser, adminUser));

        ResponseEntity<List<User>> response = authResource.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ── getUsersByRole ────────────────────────────────────────────────────

    @Test
    void getUsersByRole_patient_returnsFilteredList() {
        when(authService.getUsersByRole("Patient")).thenReturn(List.of(patientUser));

        ResponseEntity<List<User>> response = authResource.getUsersByRole("Patient");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getRole()).isEqualTo("Patient");
    }

    // ── reactivate ────────────────────────────────────────────────────────

    @Test
    void reactivate_callsServiceAndReturns200() {
        doNothing().when(authService).reactivateAccount(1);

        ResponseEntity<?> response = authResource.reactivate(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).reactivateAccount(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("message")).isEqualTo("Account reactivated successfully");
    }
}
