package com.focusfortress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubcategoryOptionDTO {
    private String name;
    private String icon;
    private String categoryName;
    private String categoryIcon;
}
