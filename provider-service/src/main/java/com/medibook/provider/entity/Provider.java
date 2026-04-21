package com.medibook.provider.entity;
 
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "providers",
    indexes = {
        @Index(name = "idx_user_id",      columnList = "userId"),
        @Index(name = "idx_specialization", columnList = "specialization"),
        @Index(name = "idx_is_verified",  columnList = "isVerified")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "userId")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long providerId;
 
    // Foreign key to users table in auth-service (no JPA join — microservice boundary)
    @Column(nullable = false, unique = true)
    private Long userId;
 
    @Column(nullable = false)
    private String specialization;
 
    @Column(nullable = false, columnDefinition = "TEXT")
    private String qualification;
 
    private Integer experienceYears;
 
    @Column(columnDefinition = "TEXT")
    private String bio;
 
    private String clinicName;
 
    @Column(columnDefinition = "TEXT")
    private String clinicAddress;
 
    @Builder.Default
    private Double avgRating = 0.0;
 
    @Builder.Default
    private Boolean isVerified = false;
 
    @Builder.Default
    private Boolean isAvailable = true;
 
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}