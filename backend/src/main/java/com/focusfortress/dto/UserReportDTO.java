package com.focusfortress.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportDTO {
    private String userName;
    private String email;
    private String generatedAt;

    // Habits
    private Integer activeHabitsCount;
    private Integer completedHabitsCount;

    // Interests
    private List<InterestInfo> interests;

    // Media Library
    private MediaLibraryStats mediaLibraryStats;

    // Counters
    private CountersStats countersStats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InterestInfo {
        private String name;
        private String emoji;
        private String categoryName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaLibraryStats {
        private Integer watchLater;
        private Integer currentlyWatching;
        private Integer finished;
        private Integer total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountersStats {
        private Integer totalCounters;
        private Integer longestStreak;
        private String longestStreakCounterName;
    }
}

