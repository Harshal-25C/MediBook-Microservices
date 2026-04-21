package com.medibook.provider.dto.response;

import lombok.Data;

/**
 * Lightweight User DTO received from auth-service via Feign.
 * Only the fields provider-service needs to build ProviderDetailResponse.
 */

@Data
public class UserResponseDto {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String role;
}