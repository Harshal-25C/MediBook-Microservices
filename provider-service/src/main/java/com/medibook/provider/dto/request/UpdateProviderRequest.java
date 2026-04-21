package com.medibook.provider.dto.request;
 
import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class UpdateProviderRequest {
 
    @Size(max = 100)
    private String specialization;
 
    @Size(max = 500)
    private String qualification;
 
    @Min(0) @Max(60)
    private Integer experienceYears;
 
    @Size(max = 1000)
    private String bio;
 
    private String clinicName;
 
    private String clinicAddress;
}