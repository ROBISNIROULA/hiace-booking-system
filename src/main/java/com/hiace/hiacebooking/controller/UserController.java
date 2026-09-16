package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.constants.Gender;
import com.hiace.hiacebooking.dto.AdminDto;
import com.hiace.hiacebooking.dto.UserDto;
import com.hiace.hiacebooking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String getLogin() {
        return "LoginForm";
    }

    @GetMapping("/signup")
    public String getSignup(Model model) {
        model.addAttribute("user", new UserDto());
        model.addAttribute("genders", Gender.values());
        return "SignupForm";
    }

    @PostMapping("/signup")
    public String postSignup(
            @Valid @ModelAttribute("user") UserDto userDto,
            BindingResult result,
            Model model) {

        // Check validation errors
        if (result.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            return "SignupForm";
        }

        // Check password match
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("passwordError", "Passwords do not match!");
            return "SignupForm";
        }

        try {
            userDto.setUserId(UUID.randomUUID().toString());
            userService.userSignup(userDto);
            // ✅ Success — redirect to login with success message
            return "redirect:/users/login?registered=true";

        } catch (RuntimeException e) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("signupError", e.getMessage());
            return "SignupForm";
        }
    }
    // ── ADMIN LOGIN ───────────────────────────────────────────────────────────
    @GetMapping("/admin/login")
    public String getAdminLogin() {
        return "admin/AdminLoginForm";
    }
    // ── ADMIN SIGNUP ──────────────────────────────────────────────────────────
    @GetMapping("/admin/signup")
    public String getAdminSignup(Model model) {
        model.addAttribute("admin", new AdminDto());
        model.addAttribute("genders", Gender.values());
        return "admin/AdminSignupForm";
    }
    @PostMapping("/admin/signup")
    public String postAdminSignup(
            @Valid @ModelAttribute("admin") AdminDto adminDto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            return "admin/AdminSignupForm";
        }

        if (!adminDto.getPassword().equals(adminDto.getConfirmPassword())) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("passwordError", "Passwords do not match!");
            return "admin/AdminSignupForm";
        }

        try {
            userService.adminSignup(adminDto);
            return "redirect:/users/admin/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("signupError", e.getMessage());
            return "admin/AdminSignupForm";
        }
    }
}