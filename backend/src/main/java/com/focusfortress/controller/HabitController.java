package com.focusfortress.controller;

import com.focusfortress.dto.CategoryTreeDTO;
import com.focusfortress.dto.HabitDTO;
import com.focusfortress.model.Habit;
import com.focusfortress.service.HabitService;
import com.focusfortress.service.HabitProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final HabitProgressService habitProgressService;

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDTO>> getHabitsTree(Principal principal) {
        return ResponseEntity.ok(habitService.getHabitsTree(principal.getName()));
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<HabitDTO>> getUserHabitsByCategory(
            @PathVariable("categoryId") Long categoryId,
            Principal principal) {
        List<Habit> habits = habitService.getUserHabitsByCategory(principal.getName(), categoryId);
        return ResponseEntity.ok(
                habits.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/by-subcategory/{subcategoryId}")
    public ResponseEntity<List<HabitDTO>> getUserHabitsBySubcategory(
            @PathVariable("subcategoryId") Long subcategoryId,
            Principal principal) {
        List<Habit> habits = habitService.getUserHabitsBySubcategory(principal.getName(), subcategoryId);
        return ResponseEntity.ok(
                habits.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/by-category/{categoryId}/by-subcategory/{subcategoryId}")
    public ResponseEntity<List<HabitDTO>> getUserHabitsByCategoryAndSubcategory(
            @PathVariable("categoryId") Long categoryId,
            @PathVariable("subcategoryId") Long subcategoryId,
            Principal principal) {
        List<Habit> habits = habitService.getUserHabitsByCategoryAndSubcategory(
                principal.getName(), categoryId, subcategoryId);
        return ResponseEntity.ok(
                habits.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping
    public ResponseEntity<HabitDTO> createHabit(@Valid @RequestBody HabitDTO habitDTO, Principal principal) {
        Habit saved = habitService.saveHabit(habitDTO, principal.getName());
        URI location = URI.create("/api/habits/" + saved.getId());
        return ResponseEntity.created(location).body(convertToDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<HabitDTO>> getUserHabits(Principal principal) {
        List<Habit> habits = habitService.getUserHabits(principal.getName());

        // return habits with current streaks
        return ResponseEntity.ok(
                habits.stream()
                        .map(habit -> {
                            HabitDTO dto = convertToDTO(habit);
                            dto.setCurrentStreak(
                                    habitProgressService.getCurrentStreak(habit.getId(), principal.getName())
                            );
                            return dto;
                        })
                        .collect(Collectors.toList())
        );
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> deleteHabit(@PathVariable("habitId") Long habitId, Principal principal) {
        habitService.deleteHabit(habitId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{habitId}/done")
    public ResponseEntity<Integer> markHabitDone(@PathVariable("habitId") Long habitId, Principal principal) {
        int streak = habitProgressService.markDone(habitId, principal.getName());
        return ResponseEntity.ok(streak);
    }

    @GetMapping("/{habitId}/streak")
    public ResponseEntity<Integer> getCurrentStreak(@PathVariable("habitId") Long habitId, Principal principal) {
        int streak = habitProgressService.getCurrentStreak(habitId, principal.getName());
        return ResponseEntity.ok(streak);
    }

    private HabitDTO convertToDTO(Habit habit) {
        HabitDTO dto = new HabitDTO();
        dto.setId(habit.getId());
        dto.setName(habit.getName());
        dto.setDescription(habit.getDescription());
        dto.setCategoryId(habit.getCategory() != null ? habit.getCategory().getId() : null);
        dto.setSubcategoryId(habit.getSubcategory() != null ? habit.getSubcategory().getId() : null);
        dto.setIcon(habit.getIcon());
        dto.setDurationDays(habit.getDurationDays());
        return dto;
    }
}