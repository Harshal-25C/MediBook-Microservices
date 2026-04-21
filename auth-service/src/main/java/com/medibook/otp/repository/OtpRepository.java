package com.medibook.otp.repository;

import com.medibook.otp.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpToken, Integer> {

    // Find latest unused OTP for this email
    Optional<OtpToken> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(String email);

    // Delete all OTPs for this email (cleanup before generating new one)
    void deleteAllByEmail(String email);
}