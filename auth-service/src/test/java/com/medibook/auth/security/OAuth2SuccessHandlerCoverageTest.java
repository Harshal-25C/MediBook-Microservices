package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2SuccessHandlerCoverageTest {

    @Test
    void existingGoogleUserRedirectsToOtpAndSendsOtp() throws Exception {
        OAuth2SuccessHandler handler = handlerWithMocks(existingUserRepository(), mock(JwtUtil.class), mock(OtpService.class));
        OtpService otpService = readField(handler, "otpService", OtpService.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.onAuthenticationSuccess(mock(HttpServletRequest.class), response, authentication("john@example.com", "John Doe", null));

        verify(otpService).generateAndSendOtp("john@example.com");
        verify(response).sendRedirect(org.mockito.ArgumentMatchers.argThat(url ->
                url.contains("/otp") && url.contains("email=john%40example.com") && url.contains("source=google")));
    }

    @Test
    void newGoogleUserRedirectsToRoleSelectionWithEncodedPicture() throws Exception {
        OtpService otpService = mock(OtpService.class);
        OAuth2SuccessHandler handler = handlerWithMocks(emptyUserRepository(), mock(JwtUtil.class), otpService);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.onAuthenticationSuccess(mock(HttpServletRequest.class), response,
                authentication("new@example.com", "New User", "https://img.example/avatar.png?x=1"));

        verify(response).sendRedirect(org.mockito.ArgumentMatchers.argThat(url ->
                url.contains("/oauth2/select-role")
                        && url.contains("email=new%40example.com")
                        && url.contains("provider=google")
                        && url.contains("picture=https%3A%2F%2Fimg.example%2Favatar.png%3Fx%3D1")));
        org.mockito.Mockito.verifyNoInteractions(otpService);
    }

    @Test
    void newGoogleUserHandlesNullPicture() throws Exception {
        OAuth2SuccessHandler handler = handlerWithMocks(emptyUserRepository(), mock(JwtUtil.class), mock(OtpService.class));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.onAuthenticationSuccess(mock(HttpServletRequest.class), response, authentication("new@example.com", "New User", null));

        verify(response).sendRedirect(org.mockito.ArgumentMatchers.argThat(url -> url.contains("picture=") && url.contains("provider=google")));
    }

    private static Authentication authentication(String email, String name, String picture) {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn(email);
        when(oauth2User.getAttribute("name")).thenReturn(name);
        when(oauth2User.getAttribute("picture")).thenReturn(picture);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        return authentication;
    }

    private static UserRepository existingUserRepository() {
        User user = new User();
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        return repository;
    }

    private static UserRepository emptyUserRepository() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        return repository;
    }

    private static OAuth2SuccessHandler handlerWithMocks(UserRepository repository, JwtUtil jwtUtil, OtpService otpService) throws Exception {
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler();
        setField(handler, "userRepository", repository);
        setField(handler, "jwtUtil", jwtUtil);
        setField(handler, "otpService", otpService);
        assertThat(handler).isNotNull();
        return handler;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = OAuth2SuccessHandler.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T readField(Object target, String name, Class<T> type) throws Exception {
        Field field = OAuth2SuccessHandler.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}