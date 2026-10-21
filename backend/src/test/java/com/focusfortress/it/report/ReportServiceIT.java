package com.focusfortress.it.report;

import com.focusfortress.dto.ReportSettingsDTO;
import com.focusfortress.dto.UserReportDTO;
import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.ReportSettings;
import com.focusfortress.model.User;
import com.focusfortress.repository.ReportSettingsRepository;
import com.focusfortress.service.InterestService;
import com.focusfortress.service.ReportService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReportService - progress report aggregation, settings and e-mail")
class ReportServiceIT extends MySqlIntegrationTestBase {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportSettingsRepository reportSettingsRepository;

    @Autowired
    private InterestService interestService;

    @Test
    @DisplayName("report aggregates habits, interests, media and counters of exactly this user")
    void aggregatesAllSections() {
        User user = seededUser("reporter@focusfortress.test");
        seededUser("someone.else@focusfortress.test"); // noise that must not leak into the report

        UserReportDTO report = reportService.generateReport(user, new ReportSettingsDTO());

        assertThat(report.getUserName()).isEqualTo("Test User");
        assertThat(report.getEmail()).isEqualTo("reporter@focusfortress.test");
        assertThat(report.getActiveHabitsCount()).isEqualTo(2);
        assertThat(report.getCompletedHabitsCount()).isEqualTo(1);

        assertThat(report.getInterests())
                .extracting(UserReportDTO.InterestInfo::getName, UserReportDTO.InterestInfo::getCategoryName)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Meditation", "Mind & Body"),
                        org.assertj.core.groups.Tuple.tuple("Physics", "Natural Sciences"),
                        org.assertj.core.groups.Tuple.tuple("Programming", "Tech & AI"));

        UserReportDTO.MediaLibraryStats media = report.getMediaLibraryStats();
        assertThat(media.getWatchLater()).isEqualTo(2);
        assertThat(media.getCurrentlyWatching()).isEqualTo(1);
        assertThat(media.getFinished()).isEqualTo(1);
        assertThat(media.getTotal()).isEqualTo(4);

        UserReportDTO.CountersStats counters = report.getCountersStats();
        assertThat(counters.getTotalCounters()).isEqualTo(2);
        assertThat(counters.getLongestStreak()).isEqualTo(40);
        assertThat(counters.getLongestStreakCounterName()).isEqualTo("No sugar");
    }

    @Test
    @DisplayName("disabled sections are left out of the report (null, not zero)")
    void respectsSectionToggles() {
        User user = seededUser("partial@focusfortress.test");
        ReportSettingsDTO onlyHabits = new ReportSettingsDTO(true, false, false, false, false, false, "weekly");

        UserReportDTO report = reportService.generateReport(user, onlyHabits);

        assertThat(report.getActiveHabitsCount()).isEqualTo(2);
        assertThat(report.getCompletedHabitsCount()).isNull();
        assertThat(report.getInterests()).isNull();
        assertThat(report.getMediaLibraryStats()).isNull();
        assertThat(report.getCountersStats()).isNull();
    }

    @Test
    @DisplayName("user without any data gets an all-zero report and 'N/A' as best counter")
    void emptyUser() {
        User user = data.verifiedUser();

        UserReportDTO report = reportService.generateReport(user, new ReportSettingsDTO());

        assertThat(report.getActiveHabitsCount()).isZero();
        assertThat(report.getCompletedHabitsCount()).isZero();
        assertThat(report.getInterests()).isEmpty();
        assertThat(report.getMediaLibraryStats().getTotal()).isZero();
        assertThat(report.getCountersStats().getLongestStreak()).isZero();
        assertThat(report.getCountersStats().getLongestStreakCounterName()).isEqualTo("N/A");
    }

    @Test
    @DisplayName("saving settings twice updates the single row per user (upsert)")
    void settingsAreUpserted() {
        User user = data.verifiedUser();

        reportService.saveReportSettings(user, new ReportSettingsDTO(true, true, true, true, true, false, "weekly"));
        reportService.saveReportSettings(user, new ReportSettingsDTO(false, true, false, true, false, true, "monthly"));

        assertThat(reportSettingsRepository.count()).isEqualTo(1);
        ReportSettings stored = reportSettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(stored.isIncludeActiveHabits()).isFalse();
        assertThat(stored.isIncludeInterests()).isFalse();
        assertThat(stored.isAutomaticReports()).isTrue();
        assertThat(stored.getReportFrequency()).isEqualTo("monthly");
    }

    @Test
    @DisplayName("report_settings.user_id is UNIQUE - a second settings row for a user is rejected by MySQL")
    void oneSettingsRowPerUser() {
        User user = data.verifiedUser();
        reportService.saveReportSettings(user, new ReportSettingsDTO());

        ReportSettings duplicate = new ReportSettings();
        duplicate.setUser(user);

        assertThatThrownBy(() -> reportSettingsRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("generateAndSendReport delivers an HTML e-mail with the user's numbers")
    void reportIsSentByEmail() throws Exception {
        User user = seededUser("mailme@focusfortress.test");

        reportService.generateAndSendReport(user, new ReportSettingsDTO());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage mail = GREEN_MAIL.getReceivedMessages()[0];
        assertThat(mail.getAllRecipients()).extracting(Object::toString).containsExactly("mailme@focusfortress.test");
        assertThat(mail.getSubject()).isEqualTo("📊 Your FocusFortress Progress Report");
        String html = mail.getContent().toString();
        assertThat(html)
                .contains("Hello <strong>Test User</strong>")
                .contains("<span class=\"stat-label\">Active Habits</span><span class=\"stat-value\">2</span>")
                .contains("<span class=\"stat-label\">Completed Habits</span><span class=\"stat-value\">1</span>")
                .contains("Meditation")
                .contains("<strong>4</strong>")
                .contains("No sugar")
                .doesNotContain("%USER_NAME%", "%GENERATED_AT%");
    }

    /** 2 active + 1 completed habit, 3 interests, 4 media items, 2 counters (40 and 3 days). */
    private User seededUser(String email) {
        User user = data.verifiedUser(email);
        data.habit(user, "Meditate", 30);
        data.habit(user, "Read", 30);
        data.completedHabit(user, "Done challenge");
        interestService.createInitialInterestsForUser(user, Set.of("Meditation", "Physics", "Programming"));
        data.mediaItem(email, "v1", MediaStatus.WATCH_LATER);
        data.mediaItem(email, "v2", MediaStatus.WATCH_LATER);
        data.mediaItem(email, "v3", MediaStatus.CURRENTLY_WATCHING);
        data.mediaItem(email, "v4", MediaStatus.FINISHED);
        data.counter(user, "No sugar", LocalDateTime.now().minusDays(40).minusHours(1));
        data.counter(user, "No social media", LocalDateTime.now().minusDays(3).minusHours(1));
        return user;
    }
}
