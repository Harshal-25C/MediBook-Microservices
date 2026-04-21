package com.medibook.provider.dto.response;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDetailResponse {

    // ── From Provider table ──────────────────────────
    private int providerId;
    private int userId;
    private String specialization;
    private String qualification;
    private int experienceYears;
    private String bio;
    private String clinicName;
    private String clinicAddress;
    private double avgRating;
    
    @JsonProperty("isVerified")  // ← ADD THIS LINE
    private boolean isVerified;
    
    private boolean isAvailable;
    private LocalDate createdAt;
    private double consultationFee;

    // ── From User table ──────────────────────────────
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
}