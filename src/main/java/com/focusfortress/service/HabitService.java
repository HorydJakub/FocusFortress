package com.focusfortress.service;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    public Habit saveHabit(HabitDTO habitDTO) {
        User user = userRepository.findById(habitDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (habitRepository.existsByUserIdAndName(user.getId(), habitDTO.getName())) {
            throw new IllegalArgumentException("Habit with this name already exists for the user");
        }

        Habit habit = Habit.builder()
                .name(habitDTO.getName())
                .description(habitDTO.getDescription())
                .category(habitDTO.getCategory())
                .subcategory(habitDTO.getSubcategory())
                .imageUrl(habitDTO.getImageUrl())
                .durationDays(habitDTO.getDurationDays())
                .predefined(habitDTO.isPredefined())
                .user(user)
                .build();

        return habitRepository.save(habit);
    }

    public List<Habit> getHabitsByUserId(Long userId) {
        return habitRepository.findByUserId(userId);
    }

    public void deleteHabit(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        habitRepository.delete(habit);
    }
}