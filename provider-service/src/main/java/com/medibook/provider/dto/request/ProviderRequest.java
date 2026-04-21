package com.medibook.provider.dto.request;
 
import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class ProviderRequest {
 
    @NotNull(message = "userId is required (from auth-service registration)")
    private int userId;
 
    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must be at most 100 characters")
    private String specialization;
 
    @NotBlank(message = "Qualification is required")
    @Size(max = 500, message = "Qualification must be at most 500 characters")
    private String qualification;
 
    @Min(value = 0, message = "Experience years cannot be negative")
    @Max(value = 60, message = "Experience years cannot exceed 60")
    private int experienceYears;
 
    @Size(max = 1000, message = "Bio must be at most 1000 characters")
    private String bio;
 
    @NotBlank(message = "Clinic address is required")
    private String clinicName;
 
    @NotBlank(message = "Clinic address is required")
    private String clinicAddress;
}