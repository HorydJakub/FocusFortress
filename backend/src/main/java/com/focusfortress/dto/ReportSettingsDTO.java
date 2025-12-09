package com.focusfortress.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportSettingsDTO {
    private boolean includeActiveHabits = true;
    private boolean includeCompletedHabits = true;
    private boolean includeInterests = true;
    private boolean includeMediaLibrary = true;
    private boolean includeCounters = true;
    private boolean automaticReports = false;
    private String reportFrequency = "weekly"; // "weekly" or "monthly"
}

