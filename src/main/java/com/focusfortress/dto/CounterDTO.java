package com.focusfortress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CounterDTO {
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Size(max = 128, message = "")
    private String name;

    @Size(max = 512, message = "Description must be at most 512 characters")
    private String description;

    private LocalDateTime startDateTime;

    @Size(max = 128, message = "Icon must be at most 128 characters")
    private String icon;
}