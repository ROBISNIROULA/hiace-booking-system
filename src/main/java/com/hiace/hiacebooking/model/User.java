package com.hiace.hiacebooking.model;

import com.hiace.hiacebooking.constants.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
	
	@Id
	private String userId;
	private String firstname;
	private String lastname;
	
	@Column(unique = true)
	private String username;
	
	@Enumerated(EnumType.STRING)
	private Gender gender;
	
	@Column(unique = true)
	private String email;
	
	private String password;
	
	private String role;
	
	private String phone;
}
