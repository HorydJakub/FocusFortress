package com.focusfortress.service;

import com.focusfortress.dto.ReportSettingsDTO;
import com.focusfortress.dto.UserReportDTO;
import com.focusfortress.model.*;
import com.focusfortress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportSettingsRepository reportSettingsRepository;
    private final HabitRepository habitRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserMediaItemRepository userMediaItemRepository;
    private final CounterRepository counterRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public ReportSettings getReportSettings(User user) {
        return reportSettingsRepository.findByUser(user).orElse(null);
    }

    @Transactional
    public ReportSettings saveReportSettings(User user, ReportSettingsDTO settingsDTO) {
        ReportSettings settings = reportSettingsRepository.findByUser(user)
                .orElse(new ReportSettings());

        settings.setUser(user);
        settings.setIncludeActiveHabits(settingsDTO.isIncludeActiveHabits());
        settings.setIncludeCompletedHabits(settingsDTO.isIncludeCompletedHabits());
        settings.setIncludeInterests(settingsDTO.isIncludeInterests());
        settings.setIncludeMediaLibrary(settingsDTO.isIncludeMediaLibrary());
        settings.setIncludeCounters(settingsDTO.isIncludeCounters());
        settings.setAutomaticReports(settingsDTO.isAutomaticReports());
        settings.setReportFrequency(settingsDTO.getReportFrequency());

        return reportSettingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public UserReportDTO generateReport(User user, ReportSettingsDTO settings) {
        System.out.println("=== Generating Report for user: " + user.getEmail() + " ===");
        System.out.println("Settings: activeHabits=" + settings.isIncludeActiveHabits() +
                ", completedHabits=" + settings.isIncludeCompletedHabits() +
                ", interests=" + settings.isIncludeInterests() +
                ", mediaLibrary=" + settings.isIncludeMediaLibrary() +
                ", counters=" + settings.isIncludeCounters());

        UserReportDTO.UserReportDTOBuilder reportBuilder = UserReportDTO.builder()
                .userName(user.getName())
                .email(user.getEmail())
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Include active habits count
        if (settings.isIncludeActiveHabits()) {
            List<Habit> allHabits = habitRepository.findByUserId(user.getId());
            long activeCount = allHabits.stream()
                    .filter(h -> !h.isDone())
                    .count();
            System.out.println("Active Habits: " + activeCount + " (from " + allHabits.size() + " total)");
            reportBuilder.activeHabitsCount((int) activeCount);
        }

        // Include completed habits count
        if (settings.isIncludeCompletedHabits()) {
            List<Habit> allHabits = habitRepository.findByUserId(user.getId());
            long completedCount = allHabits.stream()
                    .filter(Habit::isDone)
                    .count();
            System.out.println("Completed Habits: " + completedCount);
            reportBuilder.completedHabitsCount((int) completedCount);
        }

        // Include interests
        if (settings.isIncludeInterests()) {
            List<UserInterest> userInterests = userInterestRepository.findByUserIdOrderBySelectedAtDesc(user.getId());
            System.out.println("User Interests: " + userInterests.size() + " found");

            List<UserReportDTO.InterestInfo> interests = userInterests.stream()
                    .map(ui -> UserReportDTO.InterestInfo.builder()
                            .name(ui.getSubcategory().getName())
                            .emoji(ui.getSubcategory().getIcon())
                            .categoryName(ui.getSubcategory().getCategory().getName())
                            .build())
                    .collect(Collectors.toList());
            reportBuilder.interests(interests);
        }

        // Include media library stats
        if (settings.isIncludeMediaLibrary()) {
            List<UserMediaItem> mediaItems = userMediaItemRepository.findByUserEmailOrderByAddedAtDesc(user.getEmail());
            System.out.println("Media Items: " + mediaItems.size() + " found");

            long watchLater = mediaItems.stream()
                    .filter(m -> m.getStatus() == MediaStatus.WATCH_LATER)
                    .count();
            long currentlyWatching = mediaItems.stream()
                    .filter(m -> m.getStatus() == MediaStatus.CURRENTLY_WATCHING)
                    .count();
            long finished = mediaItems.stream()
                    .filter(m -> m.getStatus() == MediaStatus.FINISHED)
                    .count();

            System.out.println("Media breakdown - WatchLater: " + watchLater +
                    ", CurrentlyWatching: " + currentlyWatching + ", Finished: " + finished);

            UserReportDTO.MediaLibraryStats mediaStats = UserReportDTO.MediaLibraryStats.builder()
                    .watchLater((int) watchLater)
                    .currentlyWatching((int) currentlyWatching)
                    .finished((int) finished)
                    .total(mediaItems.size())
                    .build();
            reportBuilder.mediaLibraryStats(mediaStats);
        }

        // Include counters stats
        if (settings.isIncludeCounters()) {
            List<Counter> counters = counterRepository.findByUserId(user.getId());
            int totalCounters = counters.size();
            System.out.println("Counters: " + totalCounters + " found");

            // Calculate streaks based on startDateTime
            Counter longestStreakCounter = counters.stream()
                    .max((c1, c2) -> {
                        long streak1 = java.time.temporal.ChronoUnit.DAYS.between(c1.getStartDateTime(), LocalDateTime.now());
                        long streak2 = java.time.temporal.ChronoUnit.DAYS.between(c2.getStartDateTime(), LocalDateTime.now());
                        return Long.compare(streak1, streak2);
                    })
                    .orElse(null);

            int longestStreak = 0;
            if (longestStreakCounter != null) {
                longestStreak = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        longestStreakCounter.getStartDateTime(), LocalDateTime.now());
                System.out.println("Longest streak: " + longestStreak + " days (" + longestStreakCounter.getName() + ")");
            } else {
                System.out.println("No counters with streaks found");
            }

            UserReportDTO.CountersStats countersStats = UserReportDTO.CountersStats.builder()
                    .totalCounters(totalCounters)
                    .longestStreak(longestStreak)
                    .longestStreakCounterName(longestStreakCounter != null ? longestStreakCounter.getName() : "N/A")
                    .build();
            reportBuilder.countersStats(countersStats);
        }

        UserReportDTO report = reportBuilder.build();
        System.out.println("=== Report Generated - Sections included: " +
                (report.getActiveHabitsCount() != null ? "Habits " : "") +
                (report.getInterests() != null ? "Interests " : "") +
                (report.getMediaLibraryStats() != null ? "Media " : "") +
                (report.getCountersStats() != null ? "Counters" : "") + " ===");
        return report;
    }

    @Transactional
    public void generateAndSendReport(User user, ReportSettingsDTO settings) {
        UserReportDTO report = generateReport(user, settings);
        emailService.sendProgressReport(user.getEmail(), report);
    }
}
