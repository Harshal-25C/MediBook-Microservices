
package com.medibook.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.medibook.admin.config.AdminConfig;
import com.medibook.admin.entity.User;
import com.medibook.admin.repository.UserRepository;
import com.medibook.admin.runner.AdminSeederRunner;
import com.medibook.admin.security.JwtFilter;
import com.medibook.admin.security.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class GeneratedAdminTargetedCoverageTest {
    @Test
    void seederCoversEmptyExistingAndCreatedAdmins() throws Exception {
        AdminConfig empty = new AdminConfig();
        UserRepository repo = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        new AdminSeederRunner(empty, repo, encoder).run(mock(ApplicationArguments.class));
        verifyNoInteractions(repo);

        AdminConfig config = new AdminConfig();
        AdminConfig.AdminEntry existing = new AdminConfig.AdminEntry();
        existing.setFullName("Existing"); existing.setEmail("existing@x.com"); existing.setPassword("secret1");
        AdminConfig.AdminEntry created = new AdminConfig.AdminEntry();
        created.setFullName("Created"); created.setEmail("created@x.com"); created.setPassword("secret2");
        config.setAdmins(List.of(existing, created));
        when(repo.existsByEmail("existing@x.com")).thenReturn(true);
        when(repo.existsByEmail("created@x.com")).thenReturn(false);
        when(encoder.encode("secret2")).thenReturn("hash");

        new AdminSeederRunner(config, repo, encoder).run(mock(ApplicationArguments.class));

        verify(repo).save(argThat(u -> u.getEmail().equals("created@x.com") && u.getRole().equals("Admin")));
    }

    @Test
    void jwtFilterSetsAuthenticationForValidBearerToken() throws Exception {
        JwtFilter filter = new JwtFilter();
        JwtUtil util = mock(JwtUtil.class);
        Field field = JwtFilter.class.getDeclaredField("jwtUtil");
        field.setAccessible(true);
        field.set(filter, util);
        when(util.validateToken("abc")).thenReturn(true);
        when(util.extractEmail("abc")).thenReturn("admin@x.com");
        when(util.extractRole("abc")).thenReturn("Admin");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc");

        Method method = JwtFilter.class.getDeclaredMethod("doFilterInternal", HttpServletRequest.class, HttpServletResponse.class, FilterChain.class);
        method.setAccessible(true);
        SecurityContextHolder.clearContext();
        method.invoke(filter, request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin@x.com");
        verify(chain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }
}
