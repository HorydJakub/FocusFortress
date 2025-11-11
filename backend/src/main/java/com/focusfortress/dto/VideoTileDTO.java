package com.focusfortress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoTileDTO {
    private String videoId;
    private String title;
    private String channelTitle;
    private String thumbnailUrl;
    private String description;
    private String matchedInterest;
    private String matchedInterestIcon;
}
