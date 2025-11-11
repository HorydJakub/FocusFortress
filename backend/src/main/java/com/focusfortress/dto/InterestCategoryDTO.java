package com.focusfortress.dto;

import com.focusfortress.model.CategoryStructure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterestCategoryDTO {
    private String key;  // e.g. "PHILOSOPHY_MINDFULNESS"
    private String displayName;  // e.g. "Philosophy & Mindfulness"
    private List<CategoryStructure> structure;  // Categories + Subcategories with icons
}
