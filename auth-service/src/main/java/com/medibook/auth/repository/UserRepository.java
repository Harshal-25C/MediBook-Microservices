package com.medibook.auth.repository;

import com.medibook.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(User.Role role);
    Optional<User> findByPhone(String phone);
    List<User> findByFullNameContainingIgnoreCase(String name);
    void deleteByUserId(Long userId);
}