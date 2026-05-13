package com.medibook.provider.entity;
 
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;


@Entity
@Table(name = "providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int providerId;

    @Column(nullable = false, unique = true)
    private int userId;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private String qualification;

    private int experienceYears;

    private String bio;

    @Column(nullable = false)
    private String clinicName;

    @Column(nullable = false)
    private String clinicAddress;

    @Builder.Default
    private double avgRating = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private double consultationFee = 500.0;

    @Getter(AccessLevel.NONE)
    @Column(name = "is_verified", nullable = false, columnDefinition = "bit(1) default 0")
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAvailable = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDate.now();
    }

    @JsonProperty("isVerified")
    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

}