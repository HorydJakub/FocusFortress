package com.focusfortress.controller;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.service.HabitService;
import com.focusfortress.service.HabitProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final HabitProgressService habitProgressService;

    @PostMapping
    public HabitDTO createHabit(@RequestBody HabitDTO habitDTO) {
        return habitService.saveHabit(habitDTO);
    }

    @GetMapping("/user/{userId}")
    public List<HabitDTO> getHabitsByUser(@PathVariable("userId") Long userId) {
        return habitService.getHabitsByUserId(userId);
    }

    @DeleteMapping("/{habitId}")
    public void deleteHabit(@PathVariable("habitId") Long habitId) {
        habitService.deleteHabit(habitId);
    }

    @PostMapping("/{habitId}/done")
    public int markHabitDone(@PathVariable("habitId") Long habitId) {
        return habitProgressService.markDone(habitId);
    }

    @GetMapping("/{habitId}/streak")
    public int getCurrentStreak(@PathVariable("habitId") Long habitId) {
        return habitProgressService.getCurrentStreak(habitId);
    }
}
