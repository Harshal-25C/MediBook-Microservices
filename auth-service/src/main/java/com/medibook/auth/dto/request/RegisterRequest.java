// RegisterRequest.java
package com.medibook.auth.dto.request;
import com.medibook.auth.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    @NotNull
    private User.Role role;
}
