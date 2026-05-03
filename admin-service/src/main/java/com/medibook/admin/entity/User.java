package com.medibook.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Mirrors the `users` table owned by auth-service.
 * admin-service connects to the SAME auth_db so it can seed admin
 * accounts that auth-service then uses for JWT login.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    /** Patient / Provider / Admin */
    @Column(nullable = false)
    private String role;

    /** google / null for normal login */
    private String provider;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String profilePicUrl;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
