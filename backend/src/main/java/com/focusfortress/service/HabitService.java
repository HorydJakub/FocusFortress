package com.focusfortress.service;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    public Habit saveHabit(HabitDTO habitDTO, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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

    @Transactional(readOnly = true)
    public List<Habit> getUserHabits(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return habitRepository.findByUserId(user.getId());
    }

    public void deleteHabit(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        habitRepository.delete(habit);
    }

    @Transactional(readOnly = true)
    public Habit getHabitById(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        return habit;
    }
}