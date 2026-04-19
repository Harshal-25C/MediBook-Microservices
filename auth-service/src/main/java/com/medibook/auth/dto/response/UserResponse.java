package com.medibook.auth.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

//UserResponse.java
@Data @Builder
public class UserResponse {
 private Long userId;
 private String fullName;
 private String email;
 private String phone;
 private String role;
 private Boolean isActive;
 private String profilePicUrl;
 private LocalDateTime createdAt;
}