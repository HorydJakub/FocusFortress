package com.focusfortress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomInterestDTO {
    @NotBlank(message = "Interest name is required")
    private String name;

    @NotBlank(message = "Emoji is required")
    private String emoji;
}

