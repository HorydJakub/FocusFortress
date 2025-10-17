package com.focusfortress.repository;

import com.focusfortress.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserId(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
}