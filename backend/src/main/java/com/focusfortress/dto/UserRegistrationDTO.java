package com.focusfortress.dto;

import com.focusfortress.model.InterestCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class UserRegistrationDTO {

    private static final Set<String> ALLOWED_GENDERS = Set.of("Male", "Female", "Prefer not to specify");

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name should be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String gender;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    // Selected interests (InterestCategory keys)
    @NotNull(message = "Selected interests are required")
    @Size(min = 3, max = 7, message = "Please select between 3 and 7 interests")
    private Set<String> selectedInterests;

    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }

    public boolean isGenderValid() {
        return gender == null || ALLOWED_GENDERS.contains(gender);
    }

    public boolean areInterestsValid() {
        if (selectedInterests == null || selectedInterests.size() < 3 || selectedInterests.size() > 7) {
            return false;
        }
        try {
            for (String interest : selectedInterests) {
                InterestCategory.fromString(interest);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}