package com.focusfortress.unit;

import com.focusfortress.model.Category;
import com.focusfortress.model.Counter;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.model.Subcategory;
import com.focusfortress.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Small object-mother for pure unit tests. No Spring, no persistence.
 */
public final class TestData {

    public static final String OWNER_EMAIL = "owner@focusfortress.test";
    public static final String OTHER_EMAIL = "intruder@focusfortress.test";

    private TestData() {
    }

    public static User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        return user;
    }

    public static User owner() {
        return user(1L, OWNER_EMAIL);
    }

    public static Category category(long id) {
        return Category.builder().id(id).name("Category " + id).icon("C").build();
    }

    public static Subcategory subcategory(long id, Category category) {
        return Subcategory.builder().id(id).name("Subcategory " + id).icon("S").category(category).build();
    }

    public static Habit habit(long id, User user, int durationDays) {
        return Habit.builder()
                .id(id)
                .name("Habit " + id)
                .description("desc")
                .icon("H")
                .durationDays(durationDays)
                .user(user)
                .done(false)
                .build();
    }

    public static HabitProgress progress(Habit habit, LocalDate date) {
        HabitProgress progress = new HabitProgress();
        progress.setHabit(habit);
        progress.setDate(date);
        return progress;
    }

    public static Counter counter(long id, User user, String name) {
        return Counter.builder()
                .id(id)
                .name(name)
                .description("desc")
                .icon("icon")
                .user(user)
                .startDateTime(LocalDateTime.of(2024, 1, 1, 8, 0))
                .build();
    }
}
