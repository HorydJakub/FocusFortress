package com.focusfortress.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeThumbnails {
    private YouTubeThumbnail high;
    private YouTubeThumbnail medium;
}
