package com.medibook.auth.repository;

import com.medibook.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    // Find reset token by token string
    Optional<PasswordResetToken> findByToken(String token);

    // Delete all reset tokens for this email (cleanup before new one)
    void deleteAllByEmail(String email);
}