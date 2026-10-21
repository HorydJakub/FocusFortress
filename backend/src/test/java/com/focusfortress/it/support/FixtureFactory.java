package com.focusfortress.it.support;

import com.focusfortress.model.Counter;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.model.UserMediaItem;
import com.focusfortress.repository.CounterRepository;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.UserMediaItemRepository;
import com.focusfortress.repository.UserRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds persisted test fixtures through the real repositories (every call commits).
 *
 * <p>Deliberately not a Spring bean: test classes live under {@code com.focusfortress} and would
 * otherwise be component-scanned into every other test context of the project.</p>
 */
public class FixtureFactory {

    public static final String DEFAULT_PASSWORD = "Secret123";

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final UserRepository users;
    private final HabitRepository habits;
    private final HabitProgressRepository progress;
    private final UserMediaItemRepository media;
    private final CounterRepository counters;
    private final PasswordEncoder passwordEncoder;

    public FixtureFactory(ApplicationContext context) {
        this.users = context.getBean(UserRepository.class);
        this.habits = context.getBean(HabitRepository.class);
        this.progress = context.getBean(HabitProgressRepository.class);
        this.media = context.getBean(UserMediaItemRepository.class);
        this.counters = context.getBean(CounterRepository.class);
        this.passwordEncoder = context.getBean(PasswordEncoder.class);
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + SEQUENCE.incrementAndGet() + "@focusfortress.test";
    }

    /** A verified (role USER) local account with {@link #DEFAULT_PASSWORD}. */
    public User verifiedUser(String email) {
        User user = new User("Test User", email, LocalDate.of(1995, 5, 17), "Prefer not to specify",
                passwordEncoder.encode(DEFAULT_PASSWORD), null);
        user.setRole(Role.USER);
        user.setTimezone("Europe/Warsaw");
        return users.save(user);
    }

    public User verifiedUser() {
        return verifiedUser(uniqueEmail("user"));
    }

    public Habit habit(User owner, String name, int durationDays) {
        return habits.save(Habit.builder()
                .name(name)
                .durationDays(durationDays)
                .user(owner)
                .done(false)
                .build());
    }

    public Habit completedHabit(User owner, String name) {
        Habit habit = habit(owner, name, 1);
        habit.setDone(true);
        return habits.save(habit);
    }

    public HabitProgress progress(Habit habit, LocalDate date) {
        HabitProgress entry = new HabitProgress();
        entry.setHabit(habit);
        entry.setDate(date);
        return progress.save(entry);
    }

    /** Progress for each of the {@code days} days before today (yesterday, the day before, ...). */
    public void progressForPreviousDays(Habit habit, int days) {
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= days; i++) {
            progress(habit, today.minusDays(i));
        }
    }

    public UserMediaItem mediaItem(String userEmail, String videoId, MediaStatus status) {
        UserMediaItem item = new UserMediaItem();
        item.setUserEmail(userEmail);
        item.setVideoId(videoId);
        item.setTitle("Video " + videoId);
        item.setStatus(status);
        return media.save(item);
    }

    public Counter counter(User owner, String name, LocalDateTime start) {
        return counters.save(Counter.builder()
                .name(name)
                .user(owner)
                .startDateTime(start)
                .build());
    }
}
