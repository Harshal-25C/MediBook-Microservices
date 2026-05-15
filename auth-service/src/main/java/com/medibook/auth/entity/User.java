package com.medibook.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    // Patient / Provider / Admin
    @Column(nullable = false)
    private String role;

    // google / github / null for normal login
    private String provider;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String profilePicUrl;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

//    public enum Role {
//        PATIENT, PROVIDER, ADMIN
//    }
//
//    public enum OAuthProvider {
//        LOCAL, GOOGLE, GITHUB
//    }
}