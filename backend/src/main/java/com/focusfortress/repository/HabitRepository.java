package com.focusfortress.repository;

import com.focusfortress.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserId(Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    List<Habit> findByUserIdAndCategoryId(Long userId, Long categoryId);
    List<Habit> findByUserIdAndSubcategoryId(Long userId, Long subcategoryId);
    List<Habit> findByUserIdAndCategoryIdAndSubcategoryId(Long userId, Long categoryId, Long subcategoryId);
    Optional<Habit> findByIdAndUserId(Long id, Long userId);
}