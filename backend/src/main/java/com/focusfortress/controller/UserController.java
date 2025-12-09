package com.focusfortress.controller;

import com.focusfortress.dto.LoginRequestDTO;
import com.focusfortress.dto.LoginResponseDTO;
import com.focusfortress.dto.UserRegistrationDTO;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.security.JwtUtil;
import com.focusfortress.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO userDTO) {
        if (!userDTO.isPasswordMatching()) {
            return ResponseEntity.badRequest().body("Passwords do not match!");
        }
        if (!userDTO.isGenderValid()) {
            return ResponseEntity.badRequest().body("Invalid gender value!");
        }

        try {
            userService.registerUser(userDTO);
            return ResponseEntity.ok("Account created successfully! Please verify your email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok("Account verified successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email not verified!");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), loginRequest.isRememberMe());

        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer"));
    }
}