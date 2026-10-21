package com.focusfortress.it.habit;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.Category;
import com.focusfortress.model.Habit;
import com.focusfortress.model.Subcategory;
import com.focusfortress.model.User;
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.SubcategoryRepository;
import com.focusfortress.service.HabitProgressService;
import com.focusfortress.service.HabitService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Habit check-ins and streak calculation ({@link HabitProgressService}) against real MySQL.
 *
 * <p>{@code LocalDate.now()} is hard-wired in the service, so history is seeded with rows dated
 * relative to today (yesterday, the day before, ...).</p>
 */
@DisplayName("HabitProgressService - check-ins and streaks")
class HabitProgressServiceIT extends MySqlIntegrationTestBase {

    @Autowired
    private HabitProgressService habitProgressService;

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Test
    @DisplayName("first check-in starts a streak of 1 and stores today's date")
    void firstCheckIn() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Drink water", 30);

        int streak = habitProgressService.markDone(habit.getId(), user.getEmail());

        assertThat(streak).isEqualTo(1);
        LocalDate stored = jdbc.queryForObject(
                "SELECT date FROM habit_progress WHERE habit_id = ?", LocalDate.class, habit.getId());
        assertThat(stored).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("check-in after 4 consecutive days extends the streak to 5")
    void consecutiveDaysExtendStreak() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Read", 30);
        data.progressForPreviousDays(habit, 4);

        assertThat(habitProgressService.markDone(habit.getId(), user.getEmail())).isEqualTo(5);
        assertThat(habitProgressService.getCurrentStreak(habit.getId(), user.getEmail())).isEqualTo(5);
    }

    @Test
    @DisplayName("a missed day resets the streak - older history does not count")
    void gapResetsStreak() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Run", 30);
        LocalDate today = LocalDate.now();
        data.progress(habit, today.minusDays(2));
        data.progress(habit, today.minusDays(3));
        data.progress(habit, today.minusDays(4));

        assertThat(habitProgressService.markDone(habit.getId(), user.getEmail())).isEqualTo(1);
    }

    @Test
    @DisplayName("reaching durationDays marks the habit as done (persisted), further check-ins are refused")
    void reachingDurationCompletesHabit() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "3-day challenge", 3);
        data.progressForPreviousDays(habit, 2);

        int streak = habitProgressService.markDone(habit.getId(), user.getEmail());

        assertThat(streak).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT done FROM habits WHERE id = ?", Boolean.class, habit.getId())).isTrue();
        assertThatThrownBy(() -> habitProgressService.markDone(habit.getId(), user.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This habit is already completed");
    }

    @Test
    @DisplayName("streak one below durationDays keeps the habit active")
    void belowDurationStaysActive() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Week of focus", 7);
        data.progressForPreviousDays(habit, 5);

        assertThat(habitProgressService.markDone(habit.getId(), user.getEmail())).isEqualTo(6);
        assertThat(habitRepository.findById(habit.getId()).orElseThrow().isDone()).isFalse();
    }

    @Test
    @DisplayName("second check-in on the same day is refused and nothing is written")
    void doubleCheckInSameDay() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Stretch", 30);
        habitProgressService.markDone(habit.getId(), user.getEmail());

        assertThatThrownBy(() -> habitProgressService.markDone(habit.getId(), user.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already marked as done today");
        assertThat(progressRows(habit)).isEqualTo(1);
    }

    @Test
    @DisplayName("another user cannot check in or read the streak of someone else's habit")
    void ownershipIsEnforced() {
        User owner = data.verifiedUser();
        User intruder = data.verifiedUser();
        Habit habit = data.habit(owner, "Private habit", 30);

        assertThatThrownBy(() -> habitProgressService.markDone(habit.getId(), intruder.getEmail()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> habitProgressService.getCurrentStreak(habit.getId(), intruder.getEmail()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(progressRows(habit)).isZero();
    }

    @Test
    @DisplayName("unknown habit id -> NotFoundException")
    void unknownHabit() {
        User user = data.verifiedUser();

        assertThatThrownBy(() -> habitProgressService.markDone(999_999L, user.getEmail()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleting a habit removes its progress history in the same transaction")
    void deleteHabitRemovesProgress() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Temporary", 30);
        data.progressForPreviousDays(habit, 5);

        habitService.deleteHabit(habit.getId(), user.getEmail());

        assertThat(habitRepository.existsById(habit.getId())).isFalse();
        assertThat(progressRows(habit)).isZero();
    }

    @Test
    @Disabled("BUG: getCurrentStreak() counts back from TODAY only, so a habit checked in on each of the last 5 days "
            + "but not yet today reports streak 0. HabitService.updateHabit relies on it ('currentStreak > 0') to lock "
            + "durationDays after the habit started - every morning before the check-in the lock is open and the "
            + "duration can be changed (the UI streak also shows 0). Expected: an unbroken streak ending yesterday "
            + "still counts as 'started'.")
    @DisplayName("duration of a started habit stays locked before today's check-in")
    void durationLockedWhileStreakAlive() {
        User user = data.verifiedUser();
        Category category = categoryRepository.save(Category.builder().name("Health").icon("H").build());
        Subcategory subcategory = subcategoryRepository.save(
                Subcategory.builder().name("Exercise").icon("E").category(category).build());
        Habit habit = data.habit(user, "Push-ups", 30);
        data.progressForPreviousDays(habit, 5);

        HabitDTO change = new HabitDTO();
        change.setName("Push-ups");
        change.setDurationDays(10);
        change.setCategoryId(category.getId());
        change.setSubcategoryId(subcategory.getId());

        assertThatThrownBy(() -> habitService.updateHabit(habit.getId(), change, user.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change duration after starting the habit");
    }

    @Test
    @Disabled("BUG: race condition - markDone() does check-then-insert without a UNIQUE(habit_id, date) constraint. "
            + "Two concurrent requests (double click, two devices) both pass findByHabitAndDate() and both INSERT, "
            + "leaving duplicate rows for one day (reproduced: 8 parallel calls -> 8 rows). Consequences, also verified: "
            + "every later markDone() for that day throws IncorrectResultSizeDataAccessException (unhandled -> HTTP 500) "
            + "and getCurrentStreak() collapses to 1 because the duplicate breaks the consecutive-day walk. "
            + "Fix: unique constraint on habit_progress(habit_id, date).")
    @DisplayName("concurrent check-ins on the same day produce exactly one progress row")
    void concurrentCheckInsCreateOneRow() throws Exception {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Concurrent", 30);
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit((Callable<Void>) () -> {
                    barrier.await();
                    try {
                        habitProgressService.markDone(habit.getId(), user.getEmail());
                    } catch (RuntimeException expectedForLosers) {
                        // "Already marked as done today" or a constraint violation are both fine
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(progressRows(habit)).isEqualTo(1);
    }

    private int progressRows(Habit habit) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM habit_progress WHERE habit_id = ?", Integer.class, habit.getId());
    }
}
