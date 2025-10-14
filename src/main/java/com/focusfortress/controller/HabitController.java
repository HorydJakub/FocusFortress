package com.focusfortress.controller;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.model.Habit;
import com.focusfortress.service.HabitService;
import com.focusfortress.service.HabitProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final HabitProgressService habitProgressService;

    @PostMapping
    public ResponseEntity<HabitDTO> createHabit(@RequestBody HabitDTO habitDTO) {
        Habit saved = habitService.saveHabit(habitDTO);
        URI location = URI.create("/api/habits/" + saved.getId());
        return ResponseEntity.created(location).body(convertToDTO(saved));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HabitDTO>> getHabitsByUser(@PathVariable("userId") Long userId) {
        List<Habit> habits = habitService.getHabitsByUserId(userId);
        return ResponseEntity.ok(
                habits.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> deleteHabit(@PathVariable("habitId") Long habitId) {
        habitService.deleteHabit(habitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{habitId}/done")
    public ResponseEntity<Integer> markHabitDone(@PathVariable("habitId") Long habitId) {
        int streak = habitProgressService.markDone(habitId);
        return ResponseEntity.ok(streak);
    }

    @GetMapping("/{habitId}/streak")
    public ResponseEntity<Integer> getCurrentStreak(@PathVariable("habitId") Long habitId) {
        int streak = habitProgressService.getCurrentStreak(habitId);
        return ResponseEntity.ok(streak);
    }

    private HabitDTO convertToDTO(Habit habit) {
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