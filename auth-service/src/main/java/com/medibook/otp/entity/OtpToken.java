package com.medibook.otp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Email of the user this OTP belongs to
    @Column(nullable = false)
    private String email;

    // 6-digit OTP code
    @Column(nullable = false)
    private String otp;

    // OTP expires after 5 minutes
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // false = not yet verified, true = already used
    @Column(nullable = false)
    private boolean used = false;

    @PrePersist
    public void prePersist() {
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }
}