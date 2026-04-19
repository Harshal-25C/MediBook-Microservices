package com.medibook.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountOtpRequest {

    @NotBlank(message = "OTP is required")
    private String otp;
}