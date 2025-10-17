package com.focusfortress.mapper;

import com.focusfortress.dto.HabitProgressDTO;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;

import java.time.LocalDate;

public class HabitProgressMapper {

    public static HabitProgress toEntity(HabitProgressDTO dto, Habit habit) {
        HabitProgress progress = new HabitProgress();
        progress.setHabit(habit);
        progress.setDate(LocalDate.parse(dto.getDate()));
        return progress;
    }

    public static HabitProgressDTO toDTO(HabitProgress progress) {
        HabitProgressDTO dto = new HabitProgressDTO();
        dto.setHabitId(progress.getHabit().getId());
        dto.setDate(progress.getDate().toString());
        return dto;
    }
}