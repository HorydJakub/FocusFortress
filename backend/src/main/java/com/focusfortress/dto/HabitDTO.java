package com.focusfortress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabitDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private Long categoryId;

    private Long subcategoryId;

    @Size(max = 5, message = "Icon must be at most 5 characters")
    private String icon;

    @Positive(message = "Duration must be positive")
    private int durationDays;

    private Integer currentStreak;

    private boolean done;
}