package com.medibook.auth.config;
 
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
 
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService — Unit Tests")
class CustomUserDetailsServiceTest {
 
    @Mock private UserRepository userRepository;
 
    @InjectMocks
    private CustomUserDetailsService service;
 
    // ── Helper ────────────────────────────────────────────────────────────────
 
    private User buildUser(String email, User.Role role, boolean active) {
        return User.builder()
                .userId(1L)
                .fullName("Test User")
                .email(email)
                .passwordHash("$2a$12$hashed")
                .role(role)
                .isActive(active)
                .provider(User.OAuthProvider.LOCAL)
                .build();
    }
 
    // ── loadUserByUsername ────────────────────────────────────────────────────
 
    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsernameTests {
 
        @Test
        @DisplayName("throws UsernameNotFoundException when email not found")
        void shouldThrow_whenEmailNotFound() {
            given(userRepository.findByEmail("ghost@gmail.com")).willReturn(Optional.empty());
 
            assertThatThrownBy(() -> service.loadUserByUsername("ghost@gmail.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("ghost@gmail.com");
        }
 
        @Test
        @DisplayName("returns UserDetails with correct username (email)")
        void shouldReturnUserDetails_withCorrectUsername() {
            User user = buildUser("user@gmail.com", User.Role.PATIENT, true);
            given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("user@gmail.com");
 
            assertThat(details.getUsername()).isEqualTo("user@gmail.com");
        }
 
        @Test
        @DisplayName("returns UserDetails with correct password hash")
        void shouldReturnUserDetails_withCorrectPasswordHash() {
            User user = buildUser("user@gmail.com", User.Role.PATIENT, true);
            given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("user@gmail.com");
 
            assertThat(details.getPassword()).isEqualTo("$2a$12$hashed");
        }
 
        @Test
        @DisplayName("PATIENT role maps to ROLE_PATIENT authority")
        void shouldMapPatientRole_toRolePatientAuthority() {
            User user = buildUser("patient@gmail.com", User.Role.PATIENT, true);
            given(userRepository.findByEmail("patient@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("patient@gmail.com");
 
            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_PATIENT");
        }
 
        @Test
        @DisplayName("PROVIDER role maps to ROLE_PROVIDER authority")
        void shouldMapProviderRole_toRoleProviderAuthority() {
            User user = buildUser("doc@gmail.com", User.Role.PROVIDER, true);
            given(userRepository.findByEmail("doc@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("doc@gmail.com");
 
            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_PROVIDER");
        }
 
        @Test
        @DisplayName("ADMIN role maps to ROLE_ADMIN authority")
        void shouldMapAdminRole_toRoleAdminAuthority() {
            User user = buildUser("admin@gmail.com", User.Role.ADMIN, true);
            given(userRepository.findByEmail("admin@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("admin@gmail.com");
 
            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
 
        @Test
        @DisplayName("isEnabled() returns true for active user")
        void shouldBeEnabled_forActiveUser() {
            User user = buildUser("active@gmail.com", User.Role.PATIENT, true);
            given(userRepository.findByEmail("active@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("active@gmail.com");
 
            assertThat(details.isEnabled()).isTrue();
        }
 
        @Test
        @DisplayName("isEnabled() returns false for deactivated user")
        void shouldBeDisabled_forInactiveUser() {
            User user = buildUser("dead@gmail.com", User.Role.PATIENT, false);
            given(userRepository.findByEmail("dead@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("dead@gmail.com");
 
            assertThat(details.isEnabled()).isFalse();
        }
 
        @Test
        @DisplayName("account flags (nonExpired, credentialsNonExpired, nonLocked) are all true")
        void shouldHaveAllAccountFlagsTrue() {
            User user = buildUser("u@gmail.com", User.Role.PATIENT, true);
            given(userRepository.findByEmail("u@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("u@gmail.com");
 
            assertThat(details.isAccountNonExpired()).isTrue();
            assertThat(details.isAccountNonLocked()).isTrue();
            assertThat(details.isCredentialsNonExpired()).isTrue();
        }
 
        @Test
        @DisplayName("exactly one authority is granted per user")
        void shouldHaveExactlyOneAuthority() {
            User user = buildUser("u@gmail.com", User.Role.ADMIN, true);
            given(userRepository.findByEmail("u@gmail.com")).willReturn(Optional.of(user));
 
            UserDetails details = service.loadUserByUsername("u@gmail.com");
 
            assertThat(details.getAuthorities()).hasSize(1);
        }
    }
}