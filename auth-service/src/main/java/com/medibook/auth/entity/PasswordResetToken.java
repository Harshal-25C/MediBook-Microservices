package com.medibook.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Email of the user requesting reset
    @Column(nullable = false)
    private String email;

    // Unique token sent in the reset link
    @Column(nullable = false, unique = true)
    private String token;

    // 6-digit OTP sent separately to email
    @Column(nullable = false)
    private String otp;

    // Token expires in 15 minutes
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // false = not yet used, true = already used
    @Column(nullable = false)
    private boolean used = false;

    @PrePersist
    public void prePersist() {
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }
}