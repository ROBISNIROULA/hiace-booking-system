package com.hiace.hiacebooking.dto;

import com.hiace.hiacebooking.constants.Gender;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserDto {
	
	private String userId;
	@NotBlank(message = "First Name is required")
	private String firstname;
	@NotBlank(message = "Last Name is required")
	private String lastname;
	@NotBlank(message = "User Name is required")
	private String username;
	@Enumerated(EnumType.STRING)
	private Gender gender;
	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	private String email;
	@Size(min = 4, max = 8, message = "password must be at least 4 and max is 8 characters long")
	private String password;
	private String confirmPassword;
	private String phone;

}
