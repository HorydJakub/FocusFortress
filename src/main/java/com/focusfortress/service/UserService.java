package com.focusfortress.service;

import com.focusfortress.dto.UserRegistrationDTO;
import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public User registerUser(UserRegistrationDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalStateException("Email already exists!");
        }

        String token = UUID.randomUUID().toString();

        User user = new User(
                userDTO.getName(),
                userDTO.getEmail(),
                userDTO.getDateOfBirth(),
                userDTO.getGender(),
                passwordEncoder.encode(userDTO.getPassword()),
                token
        );

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), token);

        return user;
    }

    @Transactional
    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid token"));
        user.setVerified(true);
        user.setRole(Role.USER);
        userRepository.save(user);
    }

}