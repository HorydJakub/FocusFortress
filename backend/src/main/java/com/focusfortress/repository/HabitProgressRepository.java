package com.focusfortress.repository;

import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitProgressRepository extends JpaRepository<HabitProgress, Long> {
    List<HabitProgress> findByHabitOrderByDateDesc(Habit habit);
    Optional<HabitProgress> findByHabitAndDate(Habit habit, LocalDate date);
}