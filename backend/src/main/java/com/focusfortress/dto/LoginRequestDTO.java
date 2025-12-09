package com.focusfortress.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    private String email;
    private String password;
    private boolean rememberMe = false; // Default to false for security
}