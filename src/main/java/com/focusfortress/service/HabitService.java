package com.focusfortress.service;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.mapper.HabitMapper;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    public HabitDTO saveHabit(HabitDTO habitDTO) {
        User user = userRepository.findById(habitDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Habit habit = HabitMapper.toEntity(habitDTO, user);
        Habit savedHabit = habitRepository.save(habit);
        return HabitMapper.toDTO(savedHabit);
    }

    public List<HabitDTO> getHabitsByUserId(Long userId) {
        List<Habit> habits = habitRepository.findByUserId(userId);
        return habits.stream()
                .map(HabitMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteHabit(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        habitRepository.delete(habit);
    }
}