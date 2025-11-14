package com.focusfortress.dto;

import com.focusfortress.model.MediaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMediaStatusDTO {

    @NotNull(message = "Status is required")
    private MediaStatus status;

    private String notes;
}