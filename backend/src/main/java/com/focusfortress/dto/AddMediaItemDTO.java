package com.focusfortress.dto;

import com.focusfortress.model.MediaStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMediaItemDTO {

    @NotBlank(message = "Video ID is required")
    private String videoId;

    @NotBlank(message = "Title is required")
    private String title;

    private String channelTitle;

    private String thumbnailUrl;

    private String description;

    private String matchedInterest;

    private MediaStatus status = MediaStatus.WATCH_LATER;

    private String notes;
}