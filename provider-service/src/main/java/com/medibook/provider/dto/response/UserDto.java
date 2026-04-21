package com.medibook.provider.dto.response;

import lombok.Data;


@Data
public class UserDto {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String role;
}