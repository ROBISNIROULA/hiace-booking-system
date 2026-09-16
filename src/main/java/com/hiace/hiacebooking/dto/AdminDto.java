package com.hiace.hiacebooking.dto;

import com.hiace.hiacebooking.constants.Gender;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDto {

    private String userId;

    @NotBlank(message = "First Name is required")
    private String firstname;

    @NotBlank(message = "Last Name is required")
    private String lastname;

    @NotBlank(message = "Username is required")
    private String username;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @Size(min = 4, max = 20, message = "Password must be 4-20 characters")
    private String password;

    private String confirmPassword;

    @NotBlank(message = "Security code is required")
    private String securityCode; // must be "4580"
}