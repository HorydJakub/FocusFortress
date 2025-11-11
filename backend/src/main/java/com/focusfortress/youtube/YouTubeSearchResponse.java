package com.focusfortress.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeSearchResponse {
    private List<YouTubeSearchItem> items;
}