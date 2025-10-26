package com.focusfortress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryTreeDTO {
    private Long id;
    private String name;
    private String icon;
    private List<HabitDTO> habits;
}