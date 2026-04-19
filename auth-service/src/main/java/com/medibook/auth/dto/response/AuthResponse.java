package com.medibook.auth.dto.response;


import lombok.Builder;
import lombok.Data;

//AuthResponse.java
@Data @Builder
public class AuthResponse {
 private String token;
 private String tokenType;
 private String email;
 private String role;
 private Long userId;
}
