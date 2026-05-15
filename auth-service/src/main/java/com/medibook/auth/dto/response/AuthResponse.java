package com.medibook.auth.dto.response;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;

    private String role;

    private int userId;

    private String fullName;

    private String message;
}
