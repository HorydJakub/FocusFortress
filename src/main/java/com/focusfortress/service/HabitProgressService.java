package com.focusfortress.service;

import com.focusfortress.dto.HabitProgressDTO;
import com.focusfortress.mapper.HabitProgressMapper;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class HabitProgressService {

    private final HabitProgressRepository habitProgressRepository;
    private final HabitRepository habitRepository;

    public int markDone(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        LocalDate today = LocalDate.now();

        if (habitProgressRepository.findByHabitAndDate(habit, today).isPresent()) {
            throw new IllegalStateException("Already marked as done today");
        }

        HabitProgress progress = new HabitProgress();
        progress.setHabit(habit);
        progress.setDate(today);
        habitProgressRepository.save(progress);

        List<HabitProgress> progressList = habitProgressRepository.findByHabitOrderByDateDesc(habit);

        int streak = 0;
        LocalDate day = today;
        for (HabitProgress hp : progressList) {
            if (hp.getDate().equals(day)) {
                streak++;
                day = day.minusDays(1);
            } else {
                break;
            }
        }

        if (streak >= habit.getDurationDays()) {

        }

        return streak;
    }

    public int getCurrentStreak(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));

        List<HabitProgress> progressList = habitProgressRepository.findByHabitOrderByDateDesc(habit);
        int streak = 0;
        LocalDate day = LocalDate.now();

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