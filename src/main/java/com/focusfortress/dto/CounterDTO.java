package com.focusfortress.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CounterDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime startDateTime;
    private String icon;
}