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

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 100, message = "Subcategory must be at most 100 characters")
    private String subcategory;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String imageUrl;

    @Positive(message = "Duration must be positive")
    private int durationDays;

    private boolean predefined;
}