package com.medibook.provider.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderResponse {

    // From Provider table
	private Long providerId;
    private Long userId;
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private String bio;
    private String clinicName;
    private String clinicAddress;
    private Double avgRating;
    
    @JsonProperty("isVerified")
    private Boolean isVerified;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private double consultationFee;

    // From User table 
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
}