package com.focusfortress.mapper;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;

public class HabitMapper {

    public static Habit toEntity(HabitDTO dto, User user) {
        Habit habit = new Habit();
        habit.setId(dto.getId());
        habit.setName(dto.getName());
        habit.setDescription(dto.getDescription());
        habit.setCategory(dto.getCategory());
        habit.setSubcategory(dto.getSubcategory());
        habit.setImageUrl(dto.getImageUrl());
        habit.setDurationDays(dto.getDurationDays());
        habit.setPredefined(dto.isPredefined());
        habit.setUser(user);
        return habit;
    }

    public static HabitDTO toDTO(Habit habit) {
        HabitDTO dto = new HabitDTO();
        dto.setId(habit.getId());
        dto.setName(habit.getName());
        dto.setDescription(habit.getDescription());
        dto.setCategory(habit.getCategory());
        dto.setSubcategory(habit.getSubcategory());
        dto.setImageUrl(habit.getImageUrl());
        dto.setDurationDays(habit.getDurationDays());
        dto.setPredefined(habit.isPredefined());
        dto.setUserId(habit.getUser() != null ? habit.getUser().getId() : null);
        return dto;
    }
}