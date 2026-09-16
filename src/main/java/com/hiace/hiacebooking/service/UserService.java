package com.hiace.hiacebooking.service;

import com.hiace.hiacebooking.dto.AdminDto;
import com.hiace.hiacebooking.dto.UserDto;

public interface UserService {

	void userSignup(UserDto userDto);
	
	void adminSignup(AdminDto adminDto);
	
	UserDto getUserByEmail(String email);
	
	UserDto getUserByUsername(String username);
}
