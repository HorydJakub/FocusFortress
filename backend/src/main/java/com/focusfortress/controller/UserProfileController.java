package com.focusfortress.controller;

import com.focusfortress.dto.ReportSettingsDTO;
import com.focusfortress.dto.UserProfileDTO;
import com.focusfortress.model.ReportSettings;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.ReportService;
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
    private final ReportService reportService;

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

    // Report Settings endpoints
    @GetMapping("/report-settings")
    public ResponseEntity<ReportSettingsDTO> getReportSettings(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ReportSettings settings = reportService.getReportSettings(user);

        if (settings == null) {
            return ResponseEntity.notFound().build();
        }

        ReportSettingsDTO dto = new ReportSettingsDTO();
        dto.setIncludeActiveHabits(settings.isIncludeActiveHabits());
        dto.setIncludeCompletedHabits(settings.isIncludeCompletedHabits());
        dto.setIncludeInterests(settings.isIncludeInterests());
        dto.setIncludeMediaLibrary(settings.isIncludeMediaLibrary());
        dto.setIncludeCounters(settings.isIncludeCounters());
        dto.setAutomaticReports(settings.isAutomaticReports());
        dto.setReportFrequency(settings.getReportFrequency());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/report-settings")
    public ResponseEntity<String> saveReportSettings(
            @RequestBody ReportSettingsDTO settingsDTO,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        reportService.saveReportSettings(user, settingsDTO);

        return ResponseEntity.ok("Report settings saved successfully");
    }

    @PostMapping("/generate-report")
    public ResponseEntity<String> generateReport(
            @RequestBody ReportSettingsDTO settingsDTO,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        reportService.generateAndSendReport(user, settingsDTO);

        return ResponseEntity.ok("Report generated and sent to your email");
    }
}
