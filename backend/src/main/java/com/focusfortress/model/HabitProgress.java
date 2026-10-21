package com.focusfortress.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "habit_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_habit_progress_habit_date", columnNames = {"habit_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabitProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Habit habit;

    private LocalDate date;
}