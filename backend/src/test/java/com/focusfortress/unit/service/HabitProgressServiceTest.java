package com.focusfortress.unit.service;

import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.service.HabitProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.focusfortress.unit.TestData.OTHER_EMAIL;
import static com.focusfortress.unit.TestData.OWNER_EMAIL;
import static com.focusfortress.unit.TestData.habit;
import static com.focusfortress.unit.TestData.owner;
import static com.focusfortress.unit.TestData.progress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Streak semantics under test: a streak is the number of consecutive days, ending on "today"
 * (server-local {@link LocalDate#now()}), that have a progress entry.
 * <p>
 * The service reads the system clock directly, so dates are built relative to a {@code today}
 * captured at the start of each test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HabitProgressService")
class HabitProgressServiceTest {

    private static final long HABIT_ID = 10L;

    @Mock
    private HabitProgressRepository habitProgressRepository;
    @Mock
    private HabitRepository habitRepository;

    @InjectMocks
    private HabitProgressService service;

    private Habit habit;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        habit = habit(HABIT_ID, owner(), 21);
        today = LocalDate.now();
    }

    /** Progress entries for today, today-1, ... today-(days-1), newest first (as the repository returns them). */
    private List<HabitProgress> consecutiveDaysEndingToday(int days) {
        return IntStream.range(0, days).mapToObj(i -> progress(habit, today.minusDays(i))).toList();
    }

    private void givenHabitExists() {
        when(habitRepository.findById(HABIT_ID)).thenReturn(Optional.of(habit));
    }

    private void givenHistory(List<HabitProgress> history) {
        when(habitProgressRepository.findByHabitOrderByDateDesc(habit)).thenReturn(history);
    }

    @Nested
    @DisplayName("getCurrentStreak")
    class GetCurrentStreak {

        @Test
        @DisplayName("is 0 for a habit without any progress")
        void noProgress() {
            givenHabitExists();
            givenHistory(List.of());

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isZero();
        }

        @ParameterizedTest(name = "{0} consecutive day(s) ending today -> streak {0}")
        @ValueSource(ints = {1, 2, 7, 30})
        @DisplayName("counts consecutive days ending today")
        void consecutiveDays(int days) {
            givenHabitExists();
            givenHistory(consecutiveDaysEndingToday(days));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isEqualTo(days);
        }

        @Test
        @DisplayName("stops counting at the first gap")
        void gapBreaksStreak() {
            givenHabitExists();
            givenHistory(List.of(
                    progress(habit, today),
                    progress(habit, today.minusDays(1)),
                    // gap: today-2 missing
                    progress(habit, today.minusDays(3)),
                    progress(habit, today.minusDays(4))));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isEqualTo(2);
        }

        @Test
        @DisplayName("ignores an entry dated in the future (e.g. clock skew) and reports 0")
        void futureEntry() {
            givenHabitExists();
            givenHistory(List.of(progress(habit, today.plusDays(1)), progress(habit, today)));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isZero();
        }

        @Test
        @DisplayName("a duplicate entry for the same day ends the streak at that day")
        void duplicateDay() {
            givenHabitExists();
            givenHistory(List.of(
                    progress(habit, today),
                    progress(habit, today),
                    progress(habit, today.minusDays(1))));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isEqualTo(1);
        }

        @Test
        @DisplayName("counts across a month/year boundary")
        void acrossCalendarBoundaries() {
            givenHabitExists();
            // 400 days always spans at least one month end, one year end and (often) a Feb 29
            givenHistory(consecutiveDaysEndingToday(400));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isEqualTo(400);
        }

        @Test
        @DisplayName("is 0 when the most recent entry was yesterday (current behaviour)")
        void yesterdayOnlyIsCurrentlyZero() {
            givenHabitExists();
            givenHistory(List.of(progress(habit, today.minusDays(1)), progress(habit, today.minusDays(2))));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isZero();
        }

        @Test
        @Disabled("BUG: getCurrentStreak counts back from today only, so a streak that is still alive "
                + "(done yesterday, not yet today) is reported as 0 until the user checks in. "
                + "This also unlocks the 'duration cannot change after start' guard in HabitService#updateHabit.")
        @DisplayName("keeps an unbroken streak alive until the end of today")
        void streakStillAliveBeforeTodaysCheckIn() {
            givenHabitExists();
            givenHistory(List.of(progress(habit, today.minusDays(1)), progress(habit, today.minusDays(2))));

            assertThat(service.getCurrentStreak(HABIT_ID, OWNER_EMAIL)).isEqualTo(2);
        }

        @Test
        @DisplayName("throws NotFoundException for an unknown habit")
        void unknownHabit() {
            when(habitRepository.findById(HABIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCurrentStreak(HABIT_ID, OWNER_EMAIL))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Habit not found");
        }

        @Test
        @DisplayName("throws ForbiddenException when the habit belongs to another user")
        void foreignHabit() {
            givenHabitExists();

            assertThatThrownBy(() -> service.getCurrentStreak(HABIT_ID, OTHER_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
            verify(habitProgressRepository, never()).findByHabitOrderByDateDesc(any());
        }
    }

    @Nested
    @DisplayName("markDone")
    class MarkDone {

        @Test
        @DisplayName("persists a progress entry dated today and returns the new streak")
        void savesTodayEntry() {
            givenHabitExists();
            when(habitProgressRepository.findByHabitAndDate(habit, today)).thenReturn(Optional.empty());
            givenHistory(consecutiveDaysEndingToday(3));

            int streak = service.markDone(HABIT_ID, OWNER_EMAIL);

            ArgumentCaptor<HabitProgress> saved = ArgumentCaptor.forClass(HabitProgress.class);
            verify(habitProgressRepository).save(saved.capture());
            assertThat(saved.getValue().getHabit()).isSameAs(habit);
            assertThat(saved.getValue().getDate()).isEqualTo(today);
            assertThat(streak).isEqualTo(3);
        }

        @ParameterizedTest(name = "duration={0}, streak after check-in={1} -> done={2}")
        @CsvSource({
                "21, 1,  false",
                "21, 20, false",
                "21, 21, true",
                "1,  1,  true",
                "5,  6,  true"
        })
        @DisplayName("completes the habit exactly when the streak reaches its duration")
        void completionThreshold(int durationDays, int streakAfter, boolean expectedDone) {
            habit.setDurationDays(durationDays);
            givenHabitExists();
            when(habitProgressRepository.findByHabitAndDate(habit, today)).thenReturn(Optional.empty());
            givenHistory(consecutiveDaysEndingToday(streakAfter));

            int streak = service.markDone(HABIT_ID, OWNER_EMAIL);

            assertThat(streak).isEqualTo(streakAfter);
            assertThat(habit.isDone()).isEqualTo(expectedDone);
            if (expectedDone) {
                verify(habitRepository).save(habit);
            } else {
                verify(habitRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("restarts the streak at 1 after a missed day")
        void restartAfterGap() {
            givenHabitExists();
            when(habitProgressRepository.findByHabitAndDate(habit, today)).thenReturn(Optional.empty());
            List<HabitProgress> history = new ArrayList<>();
            history.add(progress(habit, today));
            history.add(progress(habit, today.minusDays(2)));
            history.add(progress(habit, today.minusDays(3)));
            givenHistory(history);

            assertThat(service.markDone(HABIT_ID, OWNER_EMAIL)).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects a second check-in on the same day")
        void alreadyDoneToday() {
            givenHabitExists();
            when(habitProgressRepository.findByHabitAndDate(habit, today))
                    .thenReturn(Optional.of(progress(habit, today)));

            assertThatThrownBy(() -> service.markDone(HABIT_ID, OWNER_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Already marked as done today");
            verify(habitProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects check-ins on an already completed habit")
        void alreadyCompleted() {
            habit.setDone(true);
            givenHabitExists();

            assertThatThrownBy(() -> service.markDone(HABIT_ID, OWNER_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("This habit is already completed");
            verify(habitProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects check-ins on another user's habit")
        void foreignHabit() {
            givenHabitExists();

            assertThatThrownBy(() -> service.markDone(HABIT_ID, OTHER_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Access denied");
            verify(habitProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws NotFoundException for an unknown habit")
        void unknownHabit() {
            when(habitRepository.findById(HABIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markDone(HABIT_ID, OWNER_EMAIL))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
