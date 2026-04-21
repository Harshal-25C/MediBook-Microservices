package com.medibook.provider.dto.request;
 
import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class ProviderRegistrationRequest {
 
    @NotNull(message = "userId is required (from auth-service registration)")
    private Long userId;
 
    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must be at most 100 characters")
    private String specialization;
 
    @NotBlank(message = "Qualification is required")
    @Size(max = 500, message = "Qualification must be at most 500 characters")
    private String qualification;
 
    @Min(value = 0, message = "Experience years cannot be negative")
    @Max(value = 60, message = "Experience years cannot exceed 60")
    private Integer experienceYears;
 
    @Size(max = 1000, message = "Bio must be at most 1000 characters")
    private String bio;
 
    private String clinicName;
 
    private String clinicAddress;
}