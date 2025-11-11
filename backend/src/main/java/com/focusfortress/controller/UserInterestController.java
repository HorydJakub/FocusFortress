package com.focusfortress.controller;

import com.focusfortress.dto.ManageInterestsDTO;
import com.focusfortress.dto.UserInterestDTO;
import com.focusfortress.service.InterestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user/interests")
@RequiredArgsConstructor
public class UserInterestController {

    private final InterestService interestService;

    @GetMapping
    public ResponseEntity<List<UserInterestDTO>> getMyInterests(Principal principal) {
        return ResponseEntity.ok(interestService.getUserInterests(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<List<UserInterestDTO>> manageMyInterests(
            @Valid @RequestBody ManageInterestsDTO dto,
            Principal principal) {

        if (!dto.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        List<UserInterestDTO> updated = interestService.manageUserInterests(principal.getName(), dto);
        return ResponseEntity.ok(updated);
    }
}