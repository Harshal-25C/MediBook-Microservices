package com.medibook.admin.service.impl;

import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;
import com.medibook.admin.entity.User;
import com.medibook.admin.exception.DuplicateResourceException;
import com.medibook.admin.exception.ResourceNotFoundException;
import com.medibook.admin.repository.UserRepository;
import com.medibook.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Helpers ────────────────────────────────────────────────────────────

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .profilePicUrl(user.getProfilePicUrl())
                .build();
    }

    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    // ── AdminService implementation ────────────────────────────────────────

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByRole(String role) {
        return userRepository.findAllByRole(role)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(int userId) {
        return toResponse(findUser(userId));
    }

    @Override
    public void deactivateUser(int userId) {
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
        log.info("[AdminService] Deactivated user id={}", userId);
    }

    @Override
    public void reactivateUser(int userId) {
        User user = findUser(userId);
        user.setActive(true);
        userRepository.save(user);
        log.info("[AdminService] Reactivated user id={}", userId);
    }

    @Override
    public UserResponse addAdmin(AddAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User admin = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("Admin")
                .phone("")
                .isActive(true)
                .provider(null)
                .build();

        User saved = userRepository.save(admin);
        log.info("[AdminService] New admin added at runtime: {}", request.getEmail());
        return toResponse(saved);
    }

    @Override
    public List<UserResponse> getAllAdmins() {
        return getUsersByRole("Admin");
    }
}
