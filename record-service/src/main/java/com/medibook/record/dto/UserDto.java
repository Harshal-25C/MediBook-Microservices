package com.medibook.record.dto;

import lombok.Data;

@Data
public class UserDto {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
}
