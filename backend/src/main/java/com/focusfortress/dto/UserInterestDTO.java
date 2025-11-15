package com.focusfortress.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.focusfortress.model.Category;
import com.focusfortress.model.Subcategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestDTO {
    private Long id;
    private String subcategoryName;
    private String subcategoryIcon;
    private String categoryName;
    private String categoryIcon;
    private LocalDateTime selectedAt;

    @JsonProperty("isCustom")
    private boolean isCustom;

    private String emoji; // For custom interests

    public static UserInterestDTO fromEntity(com.focusfortress.model.UserInterest entity) {
        Subcategory subcategory = entity.getSubcategory();
        Category category = subcategory.getCategory();
        boolean isCustom = "Custom".equals(category.getName());

        UserInterestDTO dto = new UserInterestDTO();
        dto.setId(entity.getId());
        dto.setSubcategoryName(subcategory.getName());
        dto.setSubcategoryIcon(subcategory.getIcon());
        dto.setCategoryName(category.getName());
        dto.setCategoryIcon(category.getIcon());
        dto.setSelectedAt(entity.getSelectedAt());
        dto.setCustom(isCustom);
        dto.setEmoji(isCustom ? subcategory.getIcon() : null);

        return dto;
    }
}
