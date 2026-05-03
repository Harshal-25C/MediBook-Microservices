package com.medibook.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Returned when listing or viewing a user — never exposes passwordHash. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String provider;
    private boolean isActive;
    private LocalDateTime createdAt;
    private String profilePicUrl;
}
