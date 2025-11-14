package com.focusfortress.dto;

import com.focusfortress.model.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMediaItemDTO {

    private Long id;
    private String videoId;
    private String title;
    private String channelTitle;
    private String thumbnailUrl;
    private String description;
    private String matchedInterest;
    private MediaStatus status;
    private String notes;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
}