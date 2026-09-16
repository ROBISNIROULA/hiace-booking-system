package com.hiace.hiacebooking.serviceimpl;

import com.hiace.hiacebooking.dto.AdminDto;
import com.hiace.hiacebooking.dto.UserDto;
import com.hiace.hiacebooking.model.User;
import com.hiace.hiacebooking.repository.UserRepository;
import com.hiace.hiacebooking.service.UserService;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    private static final String ADMIN_SECURITY_CODE = "4580";

    @Override
    public void userSignup(UserDto userDto) {

        // Check if username already exists
        if (userRepository.findByUsername(userDto.getUsername()) != null) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.findByEmail(userDto.getEmail()) != null) {
            throw new RuntimeException("Email already registered!");
        }

        // Map MANUALLY — do not use ModelMapper here
        // because UserDto has confirmPassword but User does not
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFirstname(userDto.getFirstname());
        user.setLastname(userDto.getLastname());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setGender(userDto.getGender());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);

        System.out.println("✅ User saved: " + user.getUsername());
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) return null;
        return mapper.map(user, UserDto.class);
    }

    @Override
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return null;
        return mapper.map(user, UserDto.class);
    }

	@Override
	public void adminSignup(AdminDto adminDto) {
        // Validate security code
        if (!ADMIN_SECURITY_CODE.equals(adminDto.getSecurityCode())) {
            throw new RuntimeException(
                "Invalid security code! You are not authorized to create an admin account.");
        }

        if (userRepository.findByUsername(adminDto.getUsername()) != null) {
            throw new RuntimeException("Username already taken!");
        }
        if (userRepository.findByEmail(adminDto.getEmail()) != null) {
            throw new RuntimeException("Email already registered!");
        }

        User admin = new User();
        admin.setUserId(UUID.randomUUID().toString());
        admin.setFirstname(adminDto.getFirstname());
        admin.setLastname(adminDto.getLastname());
        admin.setUsername(adminDto.getUsername());
        admin.setEmail(adminDto.getEmail());
        admin.setGender(adminDto.getGender());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));
        admin.setRole("ROLE_ADMIN");

        userRepository.save(admin);
        System.out.println("✅ Admin saved: " + admin.getUsername());
		
	}
}