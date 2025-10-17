package com.focusfortress.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)

public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    private String imageUrl;

    @Positive(message = "Duration must be positive")
    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private boolean predefined;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}