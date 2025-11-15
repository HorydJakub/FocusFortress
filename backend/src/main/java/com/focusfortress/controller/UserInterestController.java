package com.focusfortress.controller;

import com.focusfortress.dto.CustomInterestDTO;
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

    @PostMapping("/custom")
    public ResponseEntity<UserInterestDTO> addCustomInterest(
            @Valid @RequestBody CustomInterestDTO dto,
            Principal principal) {
        UserInterestDTO created = interestService.addCustomInterest(
                principal.getName(),
                dto.getName(),
                dto.getEmoji()
        );
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> deleteInterest(
            @PathVariable("interestId") Long interestId,
            Principal principal) {
        interestService.deleteUserInterest(principal.getName(), interestId);
        return ResponseEntity.noContent().build();
    }
}