package com.focusfortress.service;

import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class HabitProgressService {

    private final HabitProgressRepository habitProgressRepository;
    private final HabitRepository habitRepository;

    public int markDone(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        LocalDate today = LocalDate.now();

        if (habitProgressRepository.findByHabitAndDate(habit, today).isPresent()) {
            throw new IllegalStateException("Already marked as done today");
        }

        HabitProgress progress = new HabitProgress();
        progress.setHabit(habit);
        progress.setDate(today);
        habitProgressRepository.save(progress);

        return calculateStreak(habit, today);
    }

    @Transactional(readOnly = true)
    public int getCurrentStreak(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        return calculateStreak(habit, LocalDate.now());
    }

    private int calculateStreak(Habit habit, LocalDate startDate) {
        List<HabitProgress> progressList = habitProgressRepository.findByHabitOrderByDateDesc(habit);
        int streak = 0;
        LocalDate day = startDate;

        for (HabitProgress hp : progressList) {
            if (hp.getDate().equals(day)) {
                streak++;
                day = day.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }
}