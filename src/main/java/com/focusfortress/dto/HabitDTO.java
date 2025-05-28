package com.focusfortress.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabitDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String subcategory;
    private String imageUrl;
    private int durationDays;
    private boolean predefined;
    private Long userId;
}
