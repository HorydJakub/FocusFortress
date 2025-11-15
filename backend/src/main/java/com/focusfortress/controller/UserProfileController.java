package com.focusfortress.controller;

import com.focusfortress.dto.UserProfileDTO;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDTO profileDTO = new UserProfileDTO(
                user.getName(),
                user.getEmail(),
                user.getDateOfBirth(),
                user.getGender()
        );

        return ResponseEntity.ok(profileDTO);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @Valid @RequestBody UserProfileDTO profileDTO,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(profileDTO.getName());
        user.setEmail(profileDTO.getEmail());
        user.setDateOfBirth(profileDTO.getDateOfBirth());
        user.setGender(profileDTO.getGender());

        userRepository.save(user);

        return ResponseEntity.ok(profileDTO);
    }
}
