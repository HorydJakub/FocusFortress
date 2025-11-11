package com.focusfortress.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeSnippet {
    private String title;
    private String channelTitle;
    private String description;
    private YouTubeThumbnails thumbnails;
    private String categoryId;
}