package com.hiace.hiacebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiace.hiacebooking.model.User;

public interface UserRepository extends JpaRepository<User, String>{
	
	User findByUsernameAndPassword(String username, String password);
	User findByEmail(String email);
	User findByUsername(String username);
}
