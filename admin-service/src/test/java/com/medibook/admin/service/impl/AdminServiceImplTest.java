package com.medibook.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;
import com.medibook.admin.entity.User;
import com.medibook.admin.exception.DuplicateResourceException;
import com.medibook.admin.exception.ResourceNotFoundException;
import com.medibook.admin.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminServiceImpl adminService;
    private User user;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(userRepository, passwordEncoder);
        user = User.builder()
                .userId(1)
                .fullName("Dr Admin")
                .email("admin@medibook.com")
                .phone("999")
                .role("Admin")
                .isActive(true)
                .build();
    }

    @Test
    void getUsersMapsEntitiesToResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findAllByRole("Admin")).thenReturn(List.of(user));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThat(adminService.getAllUsers()).extracting(UserResponse::getEmail).containsExactly(user.getEmail());
        assertThat(adminService.getAllAdmins()).extracting(UserResponse::getRole).containsExactly("Admin");
        assertThat(adminService.getUserById(1).getFullName()).isEqualTo("Dr Admin");
    }

    @Test
    void deactivateAndReactivateToggleActiveFlag() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        adminService.deactivateUser(1);
        assertThat(user.isActive()).isFalse();
        clearInvocations(userRepository);

        adminService.reactivateUser(1);
        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void addAdminEncodesPasswordAndRejectsDuplicateEmail() {
        AddAdminRequest request = new AddAdminRequest();
        request.setFullName("New Admin");
        request.setEmail("new@medibook.com");
        request.setPassword("secret123");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = adminService.addAdmin(request);

        assertThat(response.getRole()).isEqualTo("Admin");
        verify(userRepository).save(argThat(u -> u.isActive() && "hash".equals(u.getPasswordHash())));

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> adminService.addAdmin(request)).isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(argThat(u -> "duplicate".equals(u.getEmail())));
    }

    @Test
    void missingUserThrowsResourceNotFound() {
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
