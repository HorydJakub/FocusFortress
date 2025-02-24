package com.focusfortress.controller;

import com.focusfortress.dto.UserRegistrationDTO;
import com.focusfortress.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO userDTO) {
        userService.registerUser(userDTO);
        return ResponseEntity.ok("Account has been created successfully! \nPlease check your e-mail and verify your account.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok("Account verified successfully!");
    }
}
